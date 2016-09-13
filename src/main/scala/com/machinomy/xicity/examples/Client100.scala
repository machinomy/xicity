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

object Client100 extends App {
  import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
  import com.github.nscala_time.time.Imports._
  import com.machinomy.xicity.Identifier
  import com.machinomy.xicity.mac.{Address, Message, Parameters}
  import com.machinomy.xicity.network.{Client, Peer, PeerBase}

  class Dummy(identifier: Identifier, parameters: Parameters) extends Actor with ActorLogging {
    private var upstream: ActorRef = _
    private var track: Map[Int, ActorRef] = Map.empty[Int, ActorRef]

    override def preStart(): Unit = {
      val upstreamProps = PeerBase.props[Client](identifier, self, parameters)
      upstream = context.actorOf(upstreamProps, "peer")
    }

    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
        val otherIdentifier = Identifier(300)
        val id = 10
        upstream ! Message.Request(identifier, otherIdentifier, "Hello".getBytes, DateTime.now + 10.seconds, id)
        track += (id -> sender)
      case Peer.Received(payload: Message.Meaningful) =>
        payload match {
          case message: Message.Single =>
            log info s"Received single message: $message"
          case message: Message.Request =>
            log info s"Received request: $message"
            val response = Message.Response(
              from = identifier,
              to = message.from,
              text = "RESPONSE".getBytes,
              expiration = message.expiration + 2.seconds,
              id = message.id
            )
            upstream ! response
          case message: Message.Response =>
            log info s"Received response: $message"
            track.get(message.id) match {
              case Some(actorRef) =>
                track -= message.id
                val responseText = message.text.toList.mkString
                log.info(s"AAA GOT RESPONSE!!!!: $responseText")
              case None => // Do Nothing
            }
        }
      case something =>
        log.info(s"RECEIVED $something")
    }
  }

  val system = ActorSystem("xicity")
  val identifier = Identifier(100)
  val parameters = Parameters.default.copy(seeds = Set(Address("localhost")))
  system.actorOf(Props(classOf[Dummy], identifier, parameters), "dummy")
}
