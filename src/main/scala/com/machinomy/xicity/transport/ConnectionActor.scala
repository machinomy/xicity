package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import akka.io.Tcp

class ConnectionActor(endpoint: Endpoint, initialBehavior: ConnectionActor.ABehavior) extends Actor with ActorLogging {
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
  def props(endpoint: Endpoint, behavior: ABehavior) = Props(classOf[ConnectionActor], endpoint, behavior)

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
