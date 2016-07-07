package com.machinomy.xicity.transport

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, behavior: Connection.BehaviorWrap) extends Actor with ActorLogging {
  var buffer: Array[Byte] = Array.empty

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
      val b: Array[Byte] = (buffer.toList ++ byteString.toArray.toList).toArray
      Message.decode(b) match {
        case Some(decodeResult) =>
          val message = decodeResult.value
          behavior.didRead(message)
          buffer = decodeResult.remainder.toByteArray
          println(s"BUFFER: ${buffer.toList}")
        case None =>
          buffer = b
          println(s"BUFFER: ${buffer.toList}")
          log.error(s"Received ${byteString.length} bytes, can not decode yet")
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
