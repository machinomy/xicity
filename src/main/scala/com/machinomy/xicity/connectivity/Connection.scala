package com.machinomy.xicity.connectivity

import akka.actor.{Actor, ActorLogging, Props}
import akka.io.Tcp

class Connection(vertex: Vertex, initial: Behavior) extends Actor with ActorLogging {

  override def receive: Receive = evolve(initial)

  def evolve(behavior: Behavior): Receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      next(behavior.onConnect(vertex))
    case Tcp.Received(byteString) =>
      next(behavior.onRead(byteString.toArray))
    case Tcp.Closed =>
      next(behavior.onDisconnect())
  }

  def next(behavior: Behavior) = context.become(evolve(behavior))
}

object Connection {
  def props(vertex: Vertex, behavior: Behavior) = Props(classOf[Connection], vertex, behavior)
}
