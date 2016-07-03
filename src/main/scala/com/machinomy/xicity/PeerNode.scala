package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity.transport.Address

import scala.collection.mutable
import scala.util.Random

class PeerNode(identifier: Identifier, logic: ActorRef) extends Actor with ActorLogging {
  val runningClients: mutable.Map[Address, ActorRef] = mutable.Map.empty
  var serverOpt: Option[ActorRef] = None
  var herdOpt: Option[ActorRef] = None
  var routingTable: RoutingTable = RoutingTable.empty
  var started = false

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
      val handlerFactory = () => context.actorOf(PeerConnection.props(self))
      val herd = context.actorOf(PeerClientHerd.props(identifier, handlerFactory, cmd.threshold, cmd.seeds))
      herd ! PeerClientHerd.StartCommand
      herdOpt = Some(herd)
  }

  def peerReceive: Receive = {
    case PeerNode.AddRoutingTableCommand(connector, ids) =>
      log.info(s"Add $ids for $connector to RoutingTable, $identifier")
      val nextRoutingTable = routingTable + (connector -> ids.filterNot(_ == identifier))
      log.info(s"Updated routing table for $identifier is $nextRoutingTable")
      val routingTableWasEmpty = routingTable.mapping.isEmpty
      routingTable = nextRoutingTable
      if (routingTableWasEmpty && nextRoutingTable.mapping.nonEmpty) {
        if (!started) {
          logic ! PeerNode.DidStart(self)
          started = true
        }
      }
    case PeerNode.RemoveRoutingTableCommand(connector) =>
      log.info(s"Removing routing for $connector")
      routingTable -= connector
      log.info(s"New routing table: $routingTable")
    case PeerNode.AddRunningClient(connector, client) =>
      log.info(s"Adding running client for $connector...")
      runningClients += (connector -> client)
      log.info(s"Added running client for $connector")
    case PeerNode.RemoveRunningClient(connector) =>
      log.info(s"Remove running client for $connector")
      runningClients -= connector
    case PeerNode.GetKnownIdentifiersCommand(connector) =>
      sender ! (routingTable - connector).identifiers
    case PeerNode.GetIdentifierCommand =>
      sender ! identifier
    case cmd: PeerNode.SendSingleMessageCommand =>
      val closestConnectors: Set[Address] = routingTable.closestConnectors(cmd.to, identifier)
      log.info(s"For ${cmd.to} found the closest connectors: $closestConnectors")
      log.info(routingTable.toString)
      if (cmd.expiration > DateTime.now.getMillis / 1000) {
        for {
          connector <- closestConnectors
          client <- runningClients.get(connector)
        } {
          client ! PeerClient.SendSingleMessageCommand(cmd. from, cmd.to, cmd.text, cmd.expiration)
        }
      }
    case cmd @ PeerNode.ReceivedSingleMessage(from, to, text, expiration) =>
      if (to == identifier) {
        log.info(s"RECEIVED NEW SINGLE MESSAGE: $text")
        logic ! cmd
      } else {
        self ! PeerNode.SendSingleMessageCommand(from, to, text, expiration)
      }
  }

  def random(n: Int, set: Set[Address]): Set[Address] = Random.shuffle(set.toIndexedSeq).take(n).toSet
}

object PeerNode {
  sealed trait Protocol
  case class StartServerCommand(connector: Address) extends Protocol
  case class StartClientsCommand(threshold: Int, seeds: Set[Address]) extends Protocol
  case class AddRoutingTableCommand(connector: Address, ids: Set[Identifier]) extends Protocol
  case class RemoveRoutingTableCommand(connector: Address) extends Protocol
  case class AddRunningClient(connector: Address, client: ActorRef) extends Protocol
  case class RemoveRunningClient(connector: Address) extends Protocol
  case class GetKnownIdentifiersCommand(minus: Address) extends Protocol
  case object GetIdentifierCommand extends Protocol
  case class SendSingleMessageCommand(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Protocol
  case class ReceivedSingleMessage(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Protocol

  case class DidStart(node: ActorRef) extends Protocol

  sealed trait State
  case object InitialState extends State
  case object ServerState extends State
  case object ClientState extends State
  case object FullNodeState extends State

  sealed trait Data
  case object NoData extends Data
  case class ServerData(server: ActorRef) extends Data


  def props(identifier: Identifier, logic: ActorRef) = Props(classOf[PeerNode], identifier, logic)
}
