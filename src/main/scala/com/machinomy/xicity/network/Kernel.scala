package com.machinomy.xicity.network

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac._

import scala.concurrent.Future

class Kernel(identifier: Identifier, peerOpt: Option[ActorRef]) extends Actor with ActorLogging {
  var routingTable: RoutingTable = RoutingTable.empty
  var runningConnectionBehaviors: Map[Endpoint, Connection.BehaviorWrap] = Map.empty
  var isReady: Boolean = false

  override def receive: Receive = {
    case Kernel.DidAddConnection(endpoint, connectionBehavior) =>
      log.info(s"Adding connection behavior for $endpoint")
      runningConnectionBehaviors += (endpoint -> connectionBehavior)
    case Kernel.DidRemoveConnection(endpoint) =>
      log.info(s"Removing connection behavior for $endpoint")
      runningConnectionBehaviors -= endpoint
    case Kernel.DidPex(endpoint, identifiers) =>
      //log.info(s"DidPex: $endpoint, $identifiers")
      routingTable += (endpoint -> identifiers)
      if (routingTable.mapping.nonEmpty && !isReady) {
        for (peer <- peerOpt) peer ! Peer.IsReady()
        isReady = true
      }
    case Kernel.GetIdentifiers(exceptEndpoint) =>
      val identifiers = routingTable.identifiers(exceptEndpoint) + identifier
      sender ! identifiers
    case message: Message.Meaningful =>
      log.info(s"Received message: ${message.from} -> ${message.to}")
      if (message.expiration > DateTime.now) {
        if (message.to == identifier) {
          passDownstream(message)
        } else {
          relay(message)
        }
      } else {
        log.info(s"Message ${message.from} -> ${message.to} is expired")
      }
    case something =>
      throw new IllegalArgumentException(s"Received unexpected $something")
  }

  def relay(message: Message.Meaningful): Unit = {
    for {
      endpoint <- routingTable.closestEndpoints(message.to, identifier)
      connectionBehavior <- runningConnectionBehaviors.get(endpoint)
    } {
      log.info(s"Relaying message to $endpoint")
      connectionBehavior.doWrite(message)
    }
  }

  def passDownstream(message: Message.Meaningful): Unit = {
    log.info(s"Received $message to myself")
    for (peer <- peerOpt) peer ! Peer.Received(message)
  }
}

object Kernel {
  sealed trait Event
  case class DidAddConnection(endpoint: Endpoint, connectionBehavior: Connection.BehaviorWrap) extends Event
  case class DidRemoveConnection(endpoint: Endpoint) extends Event
  case class DidPex(endpoint: Endpoint, identifiers: Set[Identifier]) extends Event

  sealed trait Command extends Event
  case class GetIdentifiers(except: Endpoint) extends Command

  case class Wrap(actorRef: ActorRef, parameters: Parameters) extends ActorWrap {
    implicit val timeout = parameters.timeout

    def didAddConnection(endpoint: Endpoint, connectionBehavior: Connection.BehaviorWrap)(implicit context: ActorContext): Unit =
      actorRef ! DidAddConnection(endpoint, connectionBehavior)
    def didRemoveConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      actorRef ! DidRemoveConnection(endpoint)
    def didPex(endpoint: Endpoint, identifiers: Set[Identifier])(implicit context: ActorContext): Unit =
      actorRef ! DidPex(endpoint, identifiers)
    def getIdentifiers(except: Endpoint)(implicit context: ActorContext): Future[Set[Identifier]] =
      (actorRef ? GetIdentifiers(except: Endpoint)).mapTo[Set[Identifier]]
    def passDownstream(message: Message.Meaningful)(implicit context: ActorContext): Unit =
      actorRef ! message
  }

  def props(identifier: Identifier, peer: ActorRef) = Props(classOf[Kernel], identifier, Some(peer))

  def props(identifier: Identifier) = Props(classOf[Kernel], identifier, None)
}
