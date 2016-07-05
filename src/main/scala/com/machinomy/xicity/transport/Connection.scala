package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, behavior: Connection.BehaviorWrap) extends Actor with ActorLogging {
  override def receive: Receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      behavior.didConnect(endpoint)
    case Tcp.Received(byteString) =>
      behavior.didRead(byteString.toArray)
    case Tcp.Closed =>
      behavior.didClose()
      context.stop(self)
    case Tcp.ErrorClosed(_) =>
      behavior.didDisconnect()
      context.stop(self)
    case Tcp.PeerClosed =>
      behavior.didDisconnect()
      context.stop(self)
  }
}

object Connection {
  sealed trait Event

  /** Just instantiated a new connection. */
  case class DidConnect(endpoint: Endpoint) extends Event

  /** Connection close is initiated by the peer. */
  case class DidDisconnect() extends Event

  /** Connection close is initiated by the code. */
  case class DidClose() extends Event

  /** Received something from the peer. */
  case class DidRead(bytes: Array[Byte]) extends Event

  def props(endpoint: Endpoint, behavior: ActorRef) = Props(classOf[Connection], endpoint, behavior)

  case class BehaviorWrap(actorRef: ActorRef) extends ActorWrap {
    def didConnect(endpoint: Endpoint)(implicit sender: ActorRef) = actorRef ! DidConnect(endpoint)
    def didDisconnect()(implicit sender: ActorRef) = actorRef ! DidDisconnect()
    def didClose()(implicit sender: ActorRef) = actorRef ! DidClose()
    def didRead(bytes: Array[Byte])(implicit sender: ActorRef) = actorRef ! DidRead(bytes)
  }

  trait BehaviorActor extends Actor with ActorLogging {
    type Handle = PartialFunction[Event, Unit]

    override def receive: Receive = {
      case something => something match {
        case event: Connection.Event => handle(event)
        case anything => throw new IllegalArgumentException(s"Received unexpected $anything")
      }
    }

    def handle: Handle
  }
}
