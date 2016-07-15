package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac

class Server(node: Kernel.Wrap, parameters: mac.Parameters) extends Actor with ActorLogging {
  var serverActorOpt: Option[ActorRef] = None
  var serverBehaviorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val serverBehaviorActor = context.actorOf(mac.ServerBehavior.props(node, parameters), "server-behavior")
    serverBehaviorActorOpt = Some(serverBehaviorActor)
    val serverBehaviorWrap = mac.Server.BehaviorWrap(serverBehaviorActor)
    val serverActor = context.actorOf(mac.Server.props(parameters.serverAddress, serverBehaviorWrap), "server")
    serverActorOpt = Some(serverActor)
  }

  override def receive: Receive = {
    case message: mac.Message.Single =>
      node.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object Server extends NodeCompanion[Server] {
  def props(node: Kernel.Wrap) =
    Props(classOf[Server], node, mac.Parameters.default)

  def props(node: Kernel.Wrap, parameters: mac.Parameters) =
    Props(classOf[Server], node, parameters)


  implicit val companion: NodeCompanion[Server] = this
}
