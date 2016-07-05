package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class ClientNode(node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientMonitorProps = ClientMonitor.props(node, parameters)
    val clientMonitorActor = context.actorOf(clientMonitorProps)
    clientMonitorActorOpt = Some(clientMonitorActor)
  }

  override def receive: Receive = {
    case message: Message.Shot =>
      node.didReceive(message.from, message.to, message.text, message.expiration)
    case message: Message.MultiShot =>
      for (identifier <- message.to) {
        node.didReceive(message.from, identifier, message.text, message.expiration)
      }
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object ClientNode {
  def props(node: Node.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[ClientNode], node, parameters)
}
