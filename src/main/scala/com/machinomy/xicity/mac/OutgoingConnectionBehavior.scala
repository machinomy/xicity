/*
 * Copyright 2016 Machinomy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.machinomy.xicity.mac

import akka.actor.{Actor, ActorLogging, Props}
import com.machinomy.xicity.network.Kernel

import scala.util.Random

class OutgoingConnectionBehavior(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  import context.dispatcher

  var endpointOpt: Option[Endpoint] = None

  val tick = context.system.scheduler.schedule(parameters.tickInitialDelay, parameters.tickInterval, self, OutgoingConnectionBehavior.Tick)

  override def receive: Receive = expectConnect orElse expectFailure

  def expectConnect: Receive = {
    case Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      endpointOpt = Some(endpoint)
      kernel.didAddConnection(endpoint, Connection.BehaviorWrap(self))
      log.info(s"Connected to $endpoint, saying Hello")
      val nonce = Random.nextInt()
      val helloMessage = Message.Hello(endpoint.address, nonce)
      endpoint.write(helloMessage)
      context.become(expectHelloResponse(nonce) orElse expectFailure)
  }

  def expectFailure: Receive = {
    case Connection.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Connection.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
    case OutgoingConnectionBehavior.Tick =>
      // Do Nothing
    case something => throw new IllegalArgumentException(s"Not expected anything, got $something")
  }

  def expectHelloResponse(nonce: Int): Receive = {
    case Message.HelloResponse(myAddress, theirNonce) =>
      if (theirNonce == nonce) {
        log.info(s"Received valid HelloResponse")
        context.become(expectMessages orElse expectFailure)
      } else {
        throw new IllegalArgumentException(s"Expected HelloResponse.nonce set to $nonce, got $theirNonce")
      }
  }

  def expectMessages: Receive = {
    case OutgoingConnectionBehavior.Tick =>
      endpointOpt match {
        case Some(endpoint) =>
          for (identifiers <- kernel.getIdentifiers(endpoint)) endpoint.write(Message.Pex(identifiers))
        case None =>
          // Do Nothing
      }
    case Message.Pex(identifiers) =>
      for (endpoint <- endpointOpt) {
        kernel.didPex(endpoint, identifiers)
        for (identifiers <- kernel.getIdentifiers(endpoint)) endpoint.write(Message.PexResponse(identifiers))
      }
    case Message.PexResponse(identifiers) =>
      for (endpoint <- endpointOpt) {
        kernel.didPex(endpoint, identifiers)
      }
    case Connection.DoWrite(message) =>
      for (endpoint <- endpointOpt) {
        endpoint.write(message)
      }
    case message: Message.Meaningful =>
      kernel.passDownstream(message)
    case something =>
      throw new IllegalArgumentException(s"Received unexpected $something")
  }

  override def postStop(): Unit =
    for (endpoint <- endpointOpt) {
      kernel.didRemoveConnection(endpoint)
    }
}

object OutgoingConnectionBehavior {
  object Tick

  def props(kernel: Kernel.Wrap, parameters: Parameters) = Props(classOf[OutgoingConnectionBehavior], kernel, parameters)
}
