package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.transport.{ClientNode, Node, Parameters}

class ClientPeer(identifier: Identifier, parameters: Parameters, callbackActorRef: Option[ActorRef]) extends Actor with ActorLogging {
  var clientNodeActorRef: ActorRef = null

  override def preStart(): Unit = {
    val nodeActorRef = context.actorOf(Node.props(identifier))
    val nodeWrap = Node.Wrap(nodeActorRef, parameters)
    clientNodeActorRef = context.actorOf(ClientNode.props(nodeWrap))
  }

  override def receive: Receive = ???
}
