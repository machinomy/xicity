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

class IncomingConnectionBehavior(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  import context.dispatcher

  var endpointOpt: Option[Endpoint] = None

  override def receive: Receive = expectConnect orElse expectFailure

  def expectConnect: Receive = {
    case Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      endpointOpt = Some(endpoint)
      kernel.didAddConnection(endpoint, Connection.BehaviorWrap(self))
      log.info(s"Connected to $endpoint, waiting for Hello")
      context.become(expectHello orElse expectFailure)
  }

  def expectFailure: Receive = {
    case Connection.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Connection.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
    case something => throw new IllegalArgumentException(s"Not expected anything, got $something")
  }

  def expectHello: Receive = {
    case Message.Hello(myAddress, nonce) =>
      for (endpoint <- endpointOpt) {
        log.info(s"Received Hello, sending HelloResponse")
        endpoint.write(Message.HelloResponse(endpoint.address, nonce))
        context.become(expectMessages orElse expectFailure)
      }
  }

  def expectMessages: Receive = {
    case Connection.DoWrite(message) =>
      for (endpoint <- endpointOpt) {
        endpoint.write(message)
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
    case message: Message.Meaningful  =>
      kernel.passDownstream(message)
  }

  override def postStop(): Unit =
    for (endpoint <- endpointOpt) {
      kernel.didRemoveConnection(endpoint)
    }

}

object IncomingConnectionBehavior {
  def props(kernel: Kernel.Wrap, parameters: Parameters): Props = Props(classOf[IncomingConnectionBehavior], kernel, parameters)
}
