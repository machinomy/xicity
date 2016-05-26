package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, FSM, Props}
import com.github.nscala_time.time.Imports._

import scala.util.Random

class PeerNode(identifier: Identifier, rcv: (Identifier, Identifier, Array[Byte], Long) => Unit) extends Actor with ActorLogging {

  var serverOpt: Option[ActorRef] = None
  var herdOpt: Option[ActorRef] = None
  var routingTable: RoutingTable = RoutingTable.empty

  log.info(s"Starting PeerNode($identifier)")

  override def receive: Receive = serverReceive orElse clientReceive orElse peerReceive

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

  def peerReceive: Receive = {
    case PeerNode.AddRoutingTableCommand(connector, ids) =>
      log.info(s"Add $ids for $connector to RoutingTable, $identifier")
      val nextRoutingTable = routingTable + (connector -> ids.filterNot(_ == identifier))
      log.info(s"Updated routing table for $identifier is $nextRoutingTable")
      routingTable = nextRoutingTable
    case PeerNode.GetKnownIdentifiersCommand(connector) =>
      sender ! (routingTable - connector).identifiers
    case PeerNode.GetIdentifierCommand =>
      sender ! identifier
    case cmd: PeerNode.SendSingleMessageCommand =>
      val closestConnectors: Set[Connector] = routingTable.closestConnectors(cmd.to, identifier)
      log.info(s"For ${cmd.to} found the closest connectors: $closestConnectors")
      log.info(routingTable.toString)
      if (cmd.expiration > DateTime.now.getMillis / 1000) {
        herdOpt.foreach { actorRef =>
          actorRef ! PeerClientHerd.SendSingleMessageCommand(closestConnectors, cmd.from, cmd.to, cmd.text, cmd.expiration)
        }
        serverOpt.foreach { actorRef =>
          actorRef ! PeerServer.SendSingleMessageCommand(closestConnectors, cmd.from, cmd.to, cmd.text, cmd.expiration)
        }
      }
    case PeerNode.ReceivedSingleMessage(from, to, text, expiration) =>
      if (to == identifier) {
        log.info(s"Received new single message: $text")
        rcv(from, to, text, expiration)
      } else {
        self ! PeerNode.SendSingleMessageCommand(from, to, text, expiration)
      }
  }

  def random(n: Int, set: Set[Connector]): Set[Connector] = Random.shuffle(set.toIndexedSeq).take(n).toSet
}

object PeerNode {
  sealed trait Protocol
  case class StartServerCommand(connector: Connector) extends Protocol
  case class StartClientsCommand(threshold: Int, seeds: Set[Connector]) extends Protocol
  case class AddRoutingTableCommand(connector: Connector, ids: Set[Identifier]) extends Protocol
  case class GetKnownIdentifiersCommand(minus: Connector) extends Protocol
  case object GetIdentifierCommand extends Protocol
  case class SendSingleMessageCommand(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Protocol
  case class ReceivedSingleMessage(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Protocol

  sealed trait State
  case object InitialState extends State
  case object ServerState extends State
  case object ClientState extends State
  case object FullNodeState extends State

  sealed trait Data
  case object NoData extends Data
  case class ServerData(server: ActorRef) extends Data


  def props(identifier: Identifier, rcv: (Identifier, Identifier, Array[Byte], Long) => Unit) = Props(classOf[PeerNode], identifier, rcv)
}
