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

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp
import akka.util.ByteString
import com.machinomy.xicity.encoding.Bytes

class Connection(endpoint: Endpoint, behavior: Connection.BehaviorWrap) extends Actor with ActorLogging {
  var buffer: Array[Byte] = Array.empty[Byte]
  val MAX_BUFFER_LENGTH = 16384

  override def receive: Receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      log.info(s"Connected to $endpoint via $remoteAddress from $localAddress")
      behavior.didConnect(endpoint, remoteAddress, localAddress)
    case Tcp.Closed =>
      log.info(s"Closed")
      behavior.didClose()
      context.stop(self)
    case Tcp.ErrorClosed(_) =>
      log.info(s"Disconnected")
      behavior.didDisconnect()
      context.stop(self)
    case Tcp.PeerClosed =>
      log.info(s"Disconnected")
      behavior.didDisconnect()
      context.stop(self)
    case Tcp.Received(byteString) =>
      Bytes.decode[Message.Message](buffer ++ byteString) match {
        case Some(decodeResult) =>
          val message = decodeResult.value
          behavior.didRead(message)
          val remainder = decodeResult.remainder
          if (remainder.length > 0) self ! Tcp.Received(ByteString(remainder.toByteArray))
        case None =>
          log.error(s"Received ${byteString.length} bytes, can not decode")
          buffer = buffer ++ byteString
          if (buffer.length > MAX_BUFFER_LENGTH) {
            buffer = byteString.toArray
          }
      }
  }
}

object Connection {
  sealed trait Event

  /** Just instantiated a new connection. */
  case class DidConnect(endpoint: Endpoint, remoteAddress: InetSocketAddress, localAddress: InetSocketAddress) extends Event

  /** Connection close is initiated by the peer. */
  case class DidDisconnect() extends Event

  /** Connection close is initiated by the code. */
  case class DidClose() extends Event

  case class DoWrite(message: Message.Message) extends Event

  def props(endpoint: Endpoint, behavior: Connection.BehaviorWrap) = Props(classOf[Connection], endpoint, behavior)

  case class BehaviorWrap(actorRef: ActorRef) extends ActorWrap {
    def didConnect(endpoint: Endpoint, remoteAddress: InetSocketAddress, localAddress: InetSocketAddress)(implicit sender: ActorRef) =
      actorRef ! DidConnect(endpoint, remoteAddress, localAddress)
    def didDisconnect()(implicit sender: ActorRef) =
      actorRef ! DidDisconnect()
    def didClose()(implicit sender: ActorRef) =
      actorRef ! DidClose()
    def didRead(message: Message.Message)(implicit sender: ActorRef) =
      actorRef ! message
    def doWrite(message: Message.Message)(implicit sender: ActorRef) =
      actorRef ! DoWrite(message)
  }

  abstract class Behavior extends EventHandler[Connection.Event]
}
