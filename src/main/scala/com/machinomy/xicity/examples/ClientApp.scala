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

package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.network.{Client, Peer, PeerBase}

object ClientApp extends App {
  class Dummy extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
      case something =>
        log.info(s"RECEIVED $something")
    }
  }

  val system = ActorSystem("xicity")
  val identifier = Identifier.random
  val dummy = system.actorOf(Props(classOf[Dummy]), "dummy")
  val peerProps = PeerBase.props[Client](identifier, dummy)
  system.actorOf(peerProps, "peer")
}
