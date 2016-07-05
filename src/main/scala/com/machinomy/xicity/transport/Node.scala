package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import com.machinomy.xicity.Identifier

import scala.concurrent.Future

class Node(identifier: Identifier) extends Actor with ActorLogging {
  var routingTable: RoutingTable = RoutingTable.empty
  var runningConnectionBehaviors: Map[Endpoint, Connection.BehaviorWrap] = Map.empty

  override def receive: Receive = {
    case Node.DidAddConnection(endpoint, connectionBehavior) =>
      log.info(s"Adding connection behavior for $endpoint")
      runningConnectionBehaviors += (endpoint -> connectionBehavior)
    case Node.DidRemoveConnection(endpoint) =>
      log.info(s"Removing connection behavior for $endpoint")
      runningConnectionBehaviors -= endpoint
    case Node.DidPex(endpoint, identifiers) =>
      log.info(s"DidPex: $endpoint, $identifiers")
    case Node.GetIdentifiers(exceptEndpoint) =>
      log.info(s"Getting identifiers except $exceptEndpoint")
      val identifiers = routingTable.identifiers(exceptEndpoint) + identifier
      sender ! identifiers
  }
}

object Node {
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
  }

  def props(identifier: Identifier) = Props(classOf[Node], identifier)
}
