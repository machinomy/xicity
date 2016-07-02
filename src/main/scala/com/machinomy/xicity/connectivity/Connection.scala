package com.machinomy.xicity.connectivity

import akka.actor.{Actor, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, initialBehavior: ConnectionBehavior) extends Actor {
  override def receive: Receive = evolve(initialBehavior)

  def evolve(behavior: ConnectionBehavior): Receive = {
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

  def next(b: ConnectionBehavior) = context.become(evolve(b))
}

object Connection {
  def props(endpoint: Endpoint, state: ConnectionBehavior) = Props(classOf[Connection], endpoint, state)
}
