package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.machinomy.xicity.Identifier

trait ClientPeer extends Actor with ActorLogging {
  var clientNodeActorRef: ActorRef = null
  def identifier: Identifier
  def parameters: Parameters
  def callbackActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val nodeActorRef = context.actorOf(Node.props(identifier, self))
    val nodeWrap = Node.Wrap(nodeActorRef, parameters)
    clientNodeActorRef = context.actorOf(ClientNode.props(nodeWrap))
  }

  override def receive: Receive = {
    case Peer.IsReady() =>
      for (callbackActor <- callbackActorOpt) callbackActor ! Peer.IsReady()
    case Peer.Received(from, protocol, text, expiration) =>
      for (callbackActor <- callbackActorOpt) callbackActor ! Peer.Received(from, protocol, text, expiration)
    case anything =>
      clientNodeActorRef ! anything
  }
}
