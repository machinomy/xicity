package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac.{ClientMonitor, Message, Parameters}

class ClientNode(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientMonitorProps = ClientMonitor.props(kernel, parameters)
    val clientMonitorActor = context.actorOf(clientMonitorProps, "client-monitor")
    clientMonitorActorOpt = Some(clientMonitorActor)
  }

  override def receive: Receive = {
    case message: Message.Single =>
      kernel.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object ClientNode extends NodeCompanion[ClientNode] {
  def props(kernel: Kernel.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[ClientNode], kernel, parameters)

  implicit val companion: NodeCompanion[ClientNode] = this
}
