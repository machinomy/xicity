package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class FullNode(node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None
  var serverActorOpt: Option[ActorRef] = None
  var serverBehaviorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientMonitorProps = ClientMonitor.props(node, parameters)
    val clientMonitorActor = context.actorOf(clientMonitorProps)
    clientMonitorActorOpt = Some(clientMonitorActor)

    val serverBehaviorActor = context.actorOf(ServerBehavior.props(node, parameters))
    serverBehaviorActorOpt = Some(serverBehaviorActor)
    val serverBehaviorWrap = Server.BehaviorWrap(serverBehaviorActor)
    val serverActor = context.actorOf(Server.props(parameters.serverAddress, serverBehaviorWrap))
    serverActorOpt = Some(serverActor)
  }

  override def receive: Receive = {
    case message: Message.Shot =>
      node.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case message: Message.MultiShot =>
      for (identifier <- message.to) {
        node.didReceive(message.from, identifier, message.protocol, message.text, message.expiration)
      }
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object FullNode {
  def props(node: Node.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[FullNode], node, parameters)
}
