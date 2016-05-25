package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props}

import scala.util.Random

class PeerNode(identifier: Identifier) extends Actor with ActorLogging {

  var serverOpt: Option[ActorRef] = None
  var herdOpt: Option[ActorRef] = None
  var routingTable: RoutingTable = RoutingTable.empty

  override def receive: Receive = serverReceive orElse clientReceive

  def serverReceive: Receive = {
    case e: PeerNode.StartServerCommand =>
      log.info(s"Starting server for ${e.connector}")
      val handlerFactory = () => context.actorOf(PeerConnection.props(self))
      val server = context.actorOf(PeerServer.props(identifier, e.connector, handlerFactory))
      server ! PeerServer.StartCommand
    case c: PeerServer.DidConnect =>
      log.info(s"Serving at ${c.connector}")
      serverOpt = Some(sender)
    case e: PeerServer.CanNotBind =>
      log.warning(s"Peer Node can not be started: $e")
  }

  def clientReceive: Receive = {
    case cmd: PeerNode.StartClientsCommand =>
      log.info(s"Starting clients herd")
      val herd = context.actorOf(PeerClientHerd.props(identifier, cmd.threshold, cmd.seeds))
      herd ! PeerClientHerd.StartCommand
      herdOpt = Some(herd)
  }

  def random(n: Int, set: Set[Connector]): Set[Connector] = Random.shuffle(set.toIndexedSeq).take(n).toSet
}

object PeerNode {
  sealed trait Protocol
  case class StartServerCommand(connector: Connector) extends Protocol
  case class StartClientsCommand(threshold: Int, seeds: Set[Connector]) extends Protocol

  sealed trait State
  case object InitialState extends State
  case object ServerState extends State
  case object ClientState extends State
  case object FullNodeState extends State

  sealed trait Data
  case object NoData extends Data
  case class ServerData(server: ActorRef) extends Data


  def props(identifier: Identifier) = Props(classOf[PeerNode], identifier)
}
