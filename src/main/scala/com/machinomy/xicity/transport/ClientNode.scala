package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.Identifier

class ClientNode(identifier: Identifier, node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val selfWrap = Node.Wrap(self)
    val clientMonitorProps = ClientMonitor.props(parameters.seeds, parameters.threshold, selfWrap)
    val clientMonitorActor = context.actorOf(clientMonitorProps)
    clientMonitorActorOpt = Some(clientMonitorActor)
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

object ClientNode {
  def props(identifier: Identifier, node: Node.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[ClientNode], identifier, node, parameters)
}
