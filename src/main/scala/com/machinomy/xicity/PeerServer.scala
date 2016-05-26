package com.machinomy.xicity

import akka.actor.FSM.Failure
import akka.actor._
import akka.io.{IO, Tcp}
import com.machinomy.xicity.PeerServer.UpstreamData

case class PeerServer(id: Identifier, local: Connector, handlerFactory: () => ActorRef) extends FSM[PeerServer.State, PeerServer.Data] with ActorLogging {
  var runningClients: Map[Connector, ActorRef] = Map.empty
  import context.system

  startWith(PeerServer.InitialState, PeerServer.NoData)

  when(PeerServer.InitialState) {
    case Event(PeerServer.StartCommand, _) =>
      IO(Tcp) ! Tcp.Bind(self, local.address)
      log.info(s"Binding to $local...")
      goto(PeerServer.UpstreamBound) using PeerServer.UpstreamData(sender)
  }

  when(PeerServer.UpstreamBound) {
    case Event(Tcp.Bound(localAddress), data: UpstreamData) =>
      log.info(s"Bound to $localAddress")
      data.upstream ! PeerServer.DidConnect(Connector(localAddress))
      goto(PeerServer.FullyBound) using PeerServer.FullyBoundData(data.upstream, sender)
    case Event(Tcp.CommandFailed(cmd: Tcp.Bind), upstreamData: UpstreamData) =>
      println(upstreamData.upstream)
      upstreamData.upstream ! PeerServer.CanNotBind(Connector(cmd.localAddress))
      stop(Failure(s"Can not bind to ${cmd.localAddress}"))
  }

  when(PeerServer.FullyBound) {
    case Event(Tcp.Connected(remoteAddress, localAddress), data: PeerServer.FullyBoundData) =>
      val connection = sender
      val handler = handlerFactory()
      connection ! Tcp.Register(handler)
      log.info(s"Incoming connection from $remoteAddress")
      runningClients = runningClients.updated(Connector(remoteAddress), handler)
      handler ! PeerConnection.IncomingConnection(connection, Connector(remoteAddress), Connector(localAddress))
      stay()
    case Event(cmd: PeerServer.SendSingleMessageCommand, data: PeerServer.FullyBoundData) =>
      val handlers = for {
        connector <- cmd.closestConnectors
        handler <- runningClients.get(connector)
      } {
        log.info(s"Sending Single Message to $connector")
        handler ! PeerConnection.SingleMessage(cmd.from, cmd.to, cmd.text)
      }
      stay
  }

  whenUnhandled {
    case Event(Tcp.CommandFailed(cmd), stateData) =>
      stop(Failure(s"Command $cmd failed on $stateData while in $stateName"))
    case Event(e, s) =>
      log.info(s"Unhandled by PeerServer: ${e.toString}")
      stay
  }

  initialize()
}

object PeerServer {
  sealed trait Protocol
  case object StartCommand extends Protocol
  case class CanNotBind(connector: Connector) extends Protocol
  case class DidConnect(connector: Connector) extends Protocol
  case class SendSingleMessageCommand(closestConnectors: Set[Connector], from: Identifier, to: Identifier, text: Array[Byte]) extends Protocol

  sealed trait State
  case object InitialState extends State
  case object UpstreamBound extends State
  case object FullyBound extends State

  sealed trait Data
  case object NoData extends Data
  case class UpstreamData(upstream: ActorRef) extends Data
  case class FullyBoundData(upstream: ActorRef, downstream: ActorRef) extends Data

  def props(identifier: Identifier, local: Connector, handlerFactory: () => ActorRef) =
    Props(classOf[PeerServer], identifier, local, handlerFactory)
}
