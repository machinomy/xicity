package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import com.machinomy.xicity.Identifier

import scala.concurrent.Future

class NodeActor(initialBehavior: NodeActor.Behavior) extends Actor with ActorLogging {
  var behavior = initialBehavior
  var routingTable = RoutingTable.empty

  override def preStart(): Unit = {
    behavior = behavior.start()
  }

  override def receive: Receive = receiveNodeActorEvents orElse receiveMessages orElse receiveSomething

  def receiveNodeActorEvents: Receive = {
    case NodeActor.DidOutgoingConnection(endpoint) =>
      log.info(s"Did outgoing connection to $endpoint")

    case NodeActor.KnownIdentifiers(except) =>
      sender ! routingTable.identifiers(except)
    case NodeActor.AddIdentifiers(endpoint, identifiers) =>
      routingTable += (endpoint -> identifiers)
  }

  def receiveMessages: Receive = {
    case Message.Shot(from, to, text, expiration) =>
  }

  def receiveSomething: Receive = {
    case something => log.error(s"Got unexpected $something")
  }

  override def postStop(): Unit = {
    behavior = behavior.stop()
  }
}

object NodeActor {
  sealed trait Event
  case class DidOutgoingConnection(endpoint: Endpoint) extends Event
  case class DidOutgoingDisconnect(endpoint: Endpoint) extends Event
  case class DidOutgoingClose(endpoint: Endpoint) extends Event
  case class DidIncomingConnection(endpoint: Endpoint) extends Event
  case class DidIncomingDisconnect(endpoint: Endpoint) extends Event
  case class DidIncomingClose(endpoint: Endpoint) extends Event

  case class KnownIdentifiers(except: Endpoint) extends Event
  case class AddIdentifiers(endpoint: Endpoint, identifiers: Set[Identifier]) extends Event

  case class DidReadShot(message: Message.Shot, endpoint: Endpoint) extends Event

  trait Behavior {
    def selfActorOpt: Option[ActorRef]

    def start()(implicit context: ActorContext): Behavior
    def stop()(implicit context: ActorContext): Behavior

    def didReadMessage(message: Message.Shot, endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidReadShot(message, endpoint)

    def didOutgoingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidOutgoingConnection(endpoint)

    def didOutgoingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidOutgoingDisconnect(endpoint)

    def didOutgoingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidOutgoingClose(endpoint)

    def didIncomingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidIncomingConnection(endpoint)

    def didIncomingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidIncomingDisconnect(endpoint)

    def didIncomingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit =
      for (selfActor <- selfActorOpt) selfActor ! DidIncomingClose(endpoint)

    def didRead(endpoint: Endpoint, bytes: Array[Byte]): Unit = ???

    def knownIdentifiers(except: Endpoint): Future[Set[Identifier]] = selfActorOpt match {
      case Some(selfActor) =>
        implicit val timeout = Timeout(5.seconds)
        selfActor.ask(KnownIdentifiers(except)).mapTo[Set[Identifier]]
      case None =>
        Future.successful(Set.empty)
    }

    def addIdentifiers(endpoint: Endpoint, identifiers: Set[Identifier]): Unit =
      for (selfActor <- selfActorOpt) selfActor ! AddIdentifiers(endpoint, identifiers)
  }

  def props(behavior: Behavior) = Props(classOf[NodeActor], behavior)
}
