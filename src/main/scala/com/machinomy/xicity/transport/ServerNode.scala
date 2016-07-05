package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.Identifier

class ServerNode(identifier: Identifier, parameters: Parameters) extends Actor with ActorLogging {
  var serverActorOpt: Option[ActorRef] = None
  var serverBehaviorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val serverBehaviorActor = context.actorOf(ServerBehavior.props())
    serverBehaviorActorOpt = Some(serverBehaviorActor)
    val serverBehaviorWrap = Server.BehaviorWrap(serverBehaviorActor)
    val serverActor = context.actorOf(Server.props(parameters.serverAddress, serverBehaviorWrap))
    serverActorOpt = Some(serverActor)
  }

  override def receive: Receive = {
    case something => log.error(s"Did not expect anything, got $something")
  }
}

object ServerNode {
  def props(identifier: Identifier, parameters: Parameters = Parameters.default) =
    Props(classOf[ServerNode], identifier, parameters)
}
