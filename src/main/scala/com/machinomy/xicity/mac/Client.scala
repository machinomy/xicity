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

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

class Client(address: Address, behavior: Client.BehaviorWrap) extends Actor with ActorLogging {
  override def preStart(): Unit = {
    implicit val actorSystem = context.system
    IO(Tcp) ! Tcp.Connect(address.address)
    log.info(s"Connecting to $address")
  }

  override def receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      val endpoint = Endpoint(address, Wire(sender))
      behavior.didConnect(endpoint, remoteAddress, localAddress)
    case Tcp.CommandFailed(cmd) =>
      log.info(s"Command $cmd failed!")
      behavior.didDisconnect()
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.info("Shutting down the client...")
    behavior.didClose()
  }
}

object Client {
  sealed trait Event
  case class DidConnect(endpoint: Endpoint, remoteAddress: InetSocketAddress, localAddress: InetSocketAddress) extends Event
  case class DidDisconnect() extends Event
  case class DidClose() extends Event

  def props(address: Address, behavior: Client.BehaviorWrap) = Props(classOf[Client], address, behavior)

  case class BehaviorWrap(actorRef: ActorRef) extends ActorWrap {
    def didConnect(endpoint: Endpoint, remoteAddress: InetSocketAddress, localAddress: InetSocketAddress)(implicit context: ActorContext) =
      actorRef ! DidConnect(endpoint, remoteAddress, localAddress)
    def didDisconnect()(implicit context: ActorContext) =
      actorRef ! DidDisconnect()
    def didClose()(implicit context: ActorContext) =
      actorRef ! DidClose()
  }

  abstract class Behavior extends EventHandler[Client.Event]
}
