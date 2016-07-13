package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class ClientNode(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientMonitorProps = ClientMonitor.props(kernel, parameters)
    val clientMonitorActor = context.actorOf(clientMonitorProps)
    clientMonitorActorOpt = Some(clientMonitorActor)
  }

  override def receive: Receive = {
    case message: Message.Shot =>
      kernel.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case message: Message.MultiShot =>
      for (identifier <- message.to) {
        kernel.didReceive(message.from, identifier, message.protocol, message.text, message.expiration)
      }
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object ClientNode {
  def props(kernel: Kernel.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[ClientNode], kernel, parameters)
}
