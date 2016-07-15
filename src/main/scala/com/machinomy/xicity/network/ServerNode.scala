package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac.{Message, Parameters, Server, ServerBehavior}

class ServerNode(node: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var serverActorOpt: Option[ActorRef] = None
  var serverBehaviorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val serverBehaviorActor = context.actorOf(ServerBehavior.props(node, parameters), "server-behavior")
    serverBehaviorActorOpt = Some(serverBehaviorActor)
    val serverBehaviorWrap = Server.BehaviorWrap(serverBehaviorActor)
    val serverActor = context.actorOf(Server.props(parameters.serverAddress, serverBehaviorWrap), "server")
    serverActorOpt = Some(serverActor)
  }

  override def receive: Receive = {
    case message: Message.Single =>
      node.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object ServerNode extends NodeCompanion[ServerNode] {
  def props(node: Kernel.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[ServerNode], node, parameters)

  implicit val companion: NodeCompanion[ServerNode] = this
}
