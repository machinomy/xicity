package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.Identifier

class ServerNode(identifier: Identifier, node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var serverActorOpt: Option[ActorRef] = None
  var serverBehaviorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val selfWrap = Node.Wrap(self)
    val serverBehaviorActor = context.actorOf(ServerBehavior.props(selfWrap, parameters))
    serverBehaviorActorOpt = Some(serverBehaviorActor)
    val serverBehaviorWrap = Server.BehaviorWrap(serverBehaviorActor)
    val serverActor = context.actorOf(Server.props(parameters.serverAddress, serverBehaviorWrap))
    serverActorOpt = Some(serverActor)
  }

  override def receive: Receive = {
    case Node.DidAddConnection(endpoint, connectionBehavior) =>
      node.didAddConnection(endpoint, connectionBehavior)
    case Node.DidRemoveConnection(endpoint) =>
      node.didRemoveConnection(endpoint)
    case Node.DidPex(endpoint, identifiers) =>
      node.didPex(endpoint, identifiers)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object ServerNode {
  def props(identifier: Identifier, node: Node.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[ServerNode], identifier, node, parameters)
}
