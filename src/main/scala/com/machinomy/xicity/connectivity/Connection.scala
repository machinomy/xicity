package com.machinomy.xicity.connectivity

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, initialBehavior: Connection.Behavior) extends Actor with ActorLogging {
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
  def props(endpoint: Endpoint, behavior: Behavior) = Props(classOf[Connection], endpoint, behavior)

  trait Behavior {

    /** Just instantiated a new connection.
      *
      * @param endpoint
      * @return
      */
    def didConnect(endpoint: Endpoint): Behavior = this

    /** Connection close is initiated by the peer.
      *
      * @return
      */
    def didDisconnect(): Behavior = this

    /** Received something from the peer.
      *
      * @param bytes
      * @return
      */
    def didRead(bytes: Array[Byte]): Behavior = this

    /** Connection close is initiated by the code.
      *
      * @return
      */
    def didClose(): Behavior = this
  }
}
