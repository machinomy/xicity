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

import akka.actor.Props
import akka.io.Tcp
import com.machinomy.xicity.network.Kernel

class ServerBehavior(kernel: Kernel.Wrap, parameters: Parameters) extends Server.Behavior {
  var localAddressOpt: Option[InetSocketAddress] = None

  override def handle: Handle = {
    case Server.DidBound(localAddress) =>
      localAddressOpt = Some(localAddress)
      log.info(s"Bound to $localAddress")
    case Server.DidConnect(tcpActorRef, remoteAddress, localAddress) =>
      log.info(s"Received connection from $remoteAddress")
      val endpoint = Endpoint(Address(remoteAddress), Wire(tcpActorRef))
      val handler = newHandler(endpoint)
      tcpActorRef ! Tcp.Register(handler)
      handler ! Tcp.Connected(remoteAddress, localAddress)
      log.info(s"Server bound to $localAddressOpt got connection from $remoteAddress")
    case Server.DidDisconnect() =>
      log.info(s"Disconnected")
      localAddressOpt = None
      context.stop(self)
    case Server.DidClose() =>
      log.info(s"Closed")
      localAddressOpt = None
      context.stop(self)
  }

  def newHandler(endpoint: Endpoint) = context.actorOf(Connection.props(endpoint, connectionBehavior()))

  def connectionBehavior() = Connection.BehaviorWrap(context.actorOf(IncomingConnectionBehavior.props(kernel, parameters)))
}

object ServerBehavior {
  def props(kernel: Kernel.Wrap, parameters: Parameters) = Props(classOf[ServerBehavior], kernel, parameters)
}
