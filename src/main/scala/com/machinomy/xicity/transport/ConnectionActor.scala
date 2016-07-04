package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import akka.io.Tcp

class ConnectionActor(endpoint: Endpoint, initialBehavior: ConnectionActor.Behavior) extends Actor with ActorLogging {
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

object ConnectionActor {
  def props(endpoint: Endpoint, behavior: Behavior) = Props(classOf[ConnectionActor], endpoint, behavior)

  trait Behavior {

    /** Just instantiated a new connection.
      *
      * @param endpoint
      * @return
      */
    def didConnect(endpoint: Endpoint)(implicit context: ActorContext): Behavior

    /** Connection close is initiated by the peer.
      *
      * @return
      */
    def didDisconnect()(implicit context: ActorContext): Behavior

    /** Received something from the peer.
      *
      * @param bytes
      * @return
      */
    def didRead(bytes: Array[Byte])(implicit context: ActorContext): Behavior

    /** Connection close is initiated by the code.
      *
      * @return
      */
    def didClose()(implicit context: ActorContext): Behavior
  }
}
