package com.machinomy.xicity.connectivity

import akka.actor.{Actor, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, initialBehavior: Connection.Behavior) extends Actor {
  override def receive: Receive = evolve(initialBehavior)

  def evolve(behavior: Connection.Behavior): Receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      next(behavior.didConnect(endpoint))
    case Tcp.Received(byteString) =>
      next(behavior.didRead(byteString.toArray))
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

  def next(b: Connection.Behavior) = context.become(evolve(b))
}

object Connection {
  def props(endpoint: Endpoint, behavior: Behavior) = Props(classOf[Connection], endpoint, behavior)

  trait Behavior {
    def didConnect(endpoint: Endpoint): Behavior
    def didDisconnect(): Behavior
    def didRead(bytes: Array[Byte]): Behavior
    def didClose(): Behavior
  }
}
