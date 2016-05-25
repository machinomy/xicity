package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

case class PeerServer(id: Identifier, local: Connector, handlerFactory: () => ActorRef) extends Actor with ActorLogging {
  import context.system

  def receive = {
    case PeerServer.StartCommand =>
      IO(Tcp) ! Tcp.Bind(self, local.address)
      log.info(s"Binding to $local...")
    case Tcp.Bound(localAddress) =>
      log.info(s"Bound to $localAddress")
    case Tcp.Connected(remoteAddress, localAddress) =>
      val connection = sender
      val handler = handlerFactory()
      connection ! Tcp.Register(handler)
      log.info(s"Incoming connection from $remoteAddress")
      handler ! PeerConnection.IncomingConnection(connection, Connector(remoteAddress), Connector(localAddress))
    case Tcp.CommandFailed(cmd) =>
      log.info(s"Command $cmd failed")
      context stop self
    case e =>
      log.info(s"Unhandled by PeerServer: ${e.toString}")
  }
}

object PeerServer {
  sealed trait Protocol
  case object StartCommand extends Protocol

  sealed trait State
  case object UpstreamBound extends State
  case object FullyBound extends State

  sealed trait Data
  case class UpstreamData(upstream: ActorRef) extends Data
  case class FullyBoundData(upstream: ActorRef, downstream: ActorRef) extends Data

  def props(identifier: Identifier, local: Connector, handlerFactory: () => ActorRef) =
    Props(classOf[PeerServer], identifier, local, handlerFactory)
}
