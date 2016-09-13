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

import akka.actor._
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.Parameters

class PeerBase[A](identifier: Identifier, callback: ActorRef, parameters: Parameters, companion: NodeCompanion[A])
  extends Actor with ActorLogging {

  private var kernelActor: ActorRef = _
  private var nodeActor: ActorRef = _

  override def preStart(): Unit = {
    kernelActor = context.actorOf(Kernel.props(identifier, self), s"kernel-${identifier.number}")
    val kernelWrap = Kernel.Wrap(kernelActor, parameters)
    nodeActor = context.actorOf(companion.props(kernelWrap, parameters), "node")
  }

  override def receive: Receive = {
    case e: Peer.Callback => callback ! e
    case anything => nodeActor ! anything
  }

  override def postStop(): Unit = {
    context.stop(kernelActor)
    context.stop(nodeActor)
  }
}

object PeerBase {
  def props[A: NodeCompanion](identifier: Identifier, callback: ActorRef, parameters: Parameters): Props =
    Props(classOf[PeerBase[A]], identifier, callback, parameters, implicitly[NodeCompanion[A]])

  def props[A: NodeCompanion](identifier: Identifier, callback: ActorRef): Props =
    props[A](identifier, callback, Parameters.default)
}
