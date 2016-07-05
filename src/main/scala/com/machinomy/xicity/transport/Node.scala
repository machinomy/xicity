package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import com.machinomy.xicity.Identifier
import com.github.nscala_time.time.Imports._

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
      routingTable += (endpoint -> identifiers)
    case Node.GetIdentifiers(exceptEndpoint) =>
      log.info(s"Getting identifiers except $exceptEndpoint")
      val identifiers = routingTable.identifiers(exceptEndpoint) + identifier
      sender ! identifiers
    case Node.DidReceive(message) =>
      log.info(s"Received message $message")
      if (message.expiration > DateTime.now.getMillis / 1000) {
        if (message.to == identifier) {
          receiveToSelf(message)
        } else {
          relay(message)
        }
      } else {
        log.info(s"Message $message is expired")
      }
  }

  def relay(message: Message.Shot): Unit = {
    log.info(s"Relaying Shot from ${message.from} to ${message.to}")
    for {
      endpoint <- routingTable.closestEndpoints(message.to, identifier)
      connectionBehavior <- runningConnectionBehaviors.get(endpoint)
    } {
      log.info(s"Sending Shot to $endpoint")
      connectionBehavior.doWrite(message)
    }
  }

  def receiveToSelf(message: Message.Shot): Unit = {
    log.info(s"RECEIVED $message")
  }
}

object Node {
  sealed trait Event
  case class DidAddConnection(endpoint: Endpoint, connectionBehavior: Connection.BehaviorWrap) extends Event
  case class DidRemoveConnection(endpoint: Endpoint) extends Event
  case class DidPex(endpoint: Endpoint, identifiers: Set[Identifier]) extends Event
  case class DidReceive(message: Message.Shot) extends Command

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
    def didReceive(message: Message.Shot)(implicit context: ActorContext): Unit =
      actorRef ! DidReceive(message)
  }

  def props(identifier: Identifier) = Props(classOf[Node], identifier)
}
