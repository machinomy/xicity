package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.Identifier

class ClientNode(identifier: Identifier, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientMonitorActor = context.actorOf(ClientMonitor.props(parameters.seeds, parameters.threshold))
    clientMonitorActorOpt = Some(clientMonitorActor)
  }

  override def receive: Receive = {
    case something => log.error(s"Did not expect anything, got $something")
  }
}

object ClientNode {
  def props(identifier: Identifier, parameters: Parameters = Parameters.default) =
    Props(classOf[ClientNode], identifier, parameters)
}
