package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.Identifier

class Node extends Actor with ActorLogging {
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
  }
}

object Node {
  sealed trait Event
  case class DidAddConnection(endpoint: Endpoint, connectionBehavior: Connection.BehaviorWrap) extends Event
  case class DidRemoveConnection(endpoint: Endpoint) extends Event
  case class DidPex(endpoint: Endpoint, identifiers: Set[Identifier]) extends Event

  case class Wrap(actorRef: ActorRef) extends ActorWrap {
    def didAddConnection(endpoint: Endpoint, connectionBehavior: Connection.BehaviorWrap)(implicit context: ActorContext) =
      actorRef ! DidAddConnection(endpoint, connectionBehavior)
    def didRemoveConnection(endpoint: Endpoint)(implicit context: ActorContext) =
      actorRef ! DidRemoveConnection(endpoint)
    def didPex(endpoint: Endpoint, identifiers: Set[Identifier])(implicit context: ActorContext) =
      actorRef ! DidPex(endpoint, identifiers)
  }

  def props() = Props(classOf[Node])
}
