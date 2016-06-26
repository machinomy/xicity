package com.machinomy.xicity.connectivity

import akka.actor.{Actor, Props}
import akka.io.Tcp

class Connection(vertex: Vertex, initialState: State) extends Actor {
  override def receive: Receive = next(initialState)

  def next(state: State): Receive = evolve(state).andThen(b => context.become(next(b)))

  def evolve(state: State): PartialFunction[Any, State] = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      state.onConnect(vertex)
    case Tcp.Received(byteString) =>
      state.onRead(byteString.toArray)
    case Tcp.Closed =>
      state.onClose()
    case Tcp.ErrorClosed(_) =>
      state.onDisconnect()
    case Tcp.PeerClosed =>
      state.onDisconnect()
  }
}

object Connection {
  def props(vertex: Vertex, behavior: State) = Props(classOf[Connection], vertex, behavior)
}
