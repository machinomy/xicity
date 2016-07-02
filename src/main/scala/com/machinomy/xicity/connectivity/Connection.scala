package com.machinomy.xicity.connectivity

import akka.actor.{Actor, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, behavior: ConnectionBehavior) extends Actor {
  override def receive: Receive = evolve(behavior)

  def evolve(behavior: ConnectionBehavior): Receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      next(behavior.onConnect(endpoint))
    case Tcp.Received(byteString) =>
      next(behavior.onRead(byteString.toArray))
    case Tcp.Closed =>
      behavior.onClose()
      context.stop(self)
    case Tcp.ErrorClosed(_) =>
      behavior.onDisconnect()
      context.stop(self)
    case Tcp.PeerClosed =>
      behavior.onDisconnect()
      context.stop(self)
  }

  def next(b: ConnectionBehavior) = context.become(evolve(b))
}

object Connection {
  def props(endpoint: Endpoint, state: ConnectionBehavior) = Props(classOf[Connection], endpoint, state)
}
