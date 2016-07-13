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
      log.info(s"DidPex: $endpoint, $identifiers")
      routingTable += (endpoint -> identifiers)
      if (routingTable.mapping.nonEmpty && !isReady) {
        for (peer <- peerOpt) peer ! Peer.IsReady()
        isReady = true
      }
    case Kernel.GetIdentifiers(exceptEndpoint) =>
      log.info(s"Getting identifiers except $exceptEndpoint")
      val identifiers = routingTable.identifiers(exceptEndpoint) + identifier
      sender ! identifiers
    case Kernel.DidReceiveShot(from, to, protocol, text, expiration: Long) =>
      log.info(s"Received message: $from -> $to")
      if (expiration > DateTime.now.getMillis / 1000) {
        if (to == identifier) {
          receiveToSelf(from, to, protocol, text, expiration)
        } else {
          relay(from, to, protocol, text, expiration)
        }
      } else {
        log.info(s"Message $from -> $to is expired")
      }
  }

  def relay(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long): Unit = {
    log.info(s"Relaying Shot from $from to $to")
    for {
      endpoint <- routingTable.closestEndpoints(to, identifier)
      connectionBehavior <- runningConnectionBehaviors.get(endpoint)
    } {
      log.info(s"Sending Shot to $endpoint")
      connectionBehavior.doWrite(Message.Shot(from, to, protocol, text, expiration))
    }
  }

  def receiveToSelf(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long): Unit = {
    val message = Message.Shot(from, to, protocol, text, expiration)
    log.info(s"Received $message to myself")
    for (peer <- peerOpt) peer ! message
  }
}

object Kernel {
  sealed trait Event
  case class DidAddConnection(endpoint: Endpoint, connectionBehavior: Connection.BehaviorWrap) extends Event
  case class DidRemoveConnection(endpoint: Endpoint) extends Event
  case class DidPex(endpoint: Endpoint, identifiers: Set[Identifier]) extends Event
  case class DidReceiveShot(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long) extends Command

  sealed trait Command extends Event
  case class GetIdentifiers(except: Endpoint) extends Command

  sealed trait Callback extends Event
  case class IsReady() extends Callback

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
    def didReceive(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long)(implicit context: ActorContext): Unit =
      actorRef ! DidReceiveShot(from, to, protocol, text, expiration)
  }

  def props(identifier: Identifier, peer: ActorRef) = Props(classOf[Kernel], identifier, Some(peer))

  def props(identifier: Identifier) = Props(classOf[Kernel], identifier, None)
}
