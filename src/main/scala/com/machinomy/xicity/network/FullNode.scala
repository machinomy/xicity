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

package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac._

class FullNode(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientNodeOpt: Option[ActorRef] = None
  var serverNodeOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientNode = context.actorOf(Client.props(kernel, parameters))
    clientNodeOpt = Some(clientNode)
    val serverNode = context.actorOf(Server.props(kernel, parameters))
    serverNodeOpt = Some(serverNode)
  }

  override def receive: Receive = {
    case message: Message.Meaningful =>
      kernel.passDownstream(message)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object FullNode extends NodeCompanion[FullNode] {
  def props(kernel: Kernel.Wrap) =
    Props(classOf[FullNode], kernel, Parameters.default)

  def props(kernel: Kernel.Wrap, parameters: Parameters) =
    Props(classOf[FullNode], kernel, parameters)

  implicit val companion: NodeCompanion[FullNode] = this
}
