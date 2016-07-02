package com.machinomy.xicity.connectivity

import akka.actor.{Actor, Props}
import akka.io.Tcp

class Connection(endpoint: Endpoint, behavior: ConnectionBehavior) extends Actor {
  override def receive: Receive = next(behavior)

  def next(behavior: ConnectionBehavior): Receive = evolve(behavior).andThen(b => context.become(next(b)))

  def evolve(behavior: ConnectionBehavior): PartialFunction[Any, ConnectionBehavior] = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      behavior.onConnect(endpoint)
    case Tcp.Received(byteString) =>
      behavior.onRead(byteString.toArray)
    case Tcp.Closed =>
      behavior.onClose()
    case Tcp.ErrorClosed(_) =>
      behavior.onDisconnect()
    case Tcp.PeerClosed =>
      behavior.onDisconnect()
  }
}

object Connection {
  def props(endpoint: Endpoint, state: ConnectionBehavior) = Props(classOf[Connection], endpoint, state)
}
