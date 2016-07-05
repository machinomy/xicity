package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, initialBehavior: Connection.ABehavior) extends Actor with ActorLogging {
  var behavior = initialBehavior

  override def receive: Receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      behavior = behavior.didConnect(endpoint)
    case Tcp.Received(byteString) =>
      behavior = behavior.didRead(byteString.toArray)
    case Tcp.Closed =>
      behavior = behavior.didClose()
      context.stop(self)
    case Tcp.ErrorClosed(_) =>
      behavior = behavior.didDisconnect()
      context.stop(self)
    case Tcp.PeerClosed =>
      behavior = behavior.didDisconnect()
      context.stop(self)
  }

}

object Connection {
  sealed trait Event
  case class DidConnect(endpoint: Endpoint) extends Event
  case class DidDisconnect() extends Event
  case class DidRead(bytes: Array[Byte]) extends Event
  case class DidClose() extends Event

  def props(endpoint: Endpoint, behavior: ABehavior) = Props(classOf[Connection], endpoint, behavior)

  trait ABehavior {

    /** Just instantiated a new connection.
      *
      * @param endpoint
      * @return
      */
    def didConnect(endpoint: Endpoint)(implicit context: ActorContext): ABehavior

    /** Connection close is initiated by the peer.
      *
      * @return
      */
    def didDisconnect()(implicit context: ActorContext): ABehavior

    /** Received something from the peer.
      *
      * @param bytes
      * @return
      */
    def didRead(bytes: Array[Byte])(implicit context: ActorContext): ABehavior

    /** Connection close is initiated by the code.
      *
      * @return
      */
    def didClose()(implicit context: ActorContext): ABehavior
  }
}
