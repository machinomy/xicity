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

import akka.actor.{ActorContext, ActorRef, Props}
import akka.io.Tcp
import com.machinomy.xicity.network.Kernel

class ClientBehavior(kernel: Kernel.Wrap, parameters: Parameters) extends Client.Behavior {
  override def handle: Handle = {
    case Client.DidConnect(endpoint, remoteAddress, localAddress) =>
      log.info(s"Connected to $endpoint")
      val handler = newHandler(endpoint)
      endpoint.wire.tell(Tcp.Register(handler), context.self)
      handler ! Tcp.Connected(remoteAddress, localAddress)
    case Client.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Client.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
  }

  def newHandler(endpoint: Endpoint): ActorRef =
    context.actorOf(Connection.props(endpoint, connectionBehavior))

  def connectionBehavior()(implicit context: ActorContext) =
    Connection.BehaviorWrap(context.actorOf(OutgoingConnectionBehavior.props(kernel, parameters)))
}

object ClientBehavior {
  def props(kernel: Kernel.Wrap, parameters: Parameters) = Props(classOf[ClientBehavior], kernel, parameters)
}
