package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac._

class FullNode(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientNodeOpt: Option[ActorRef] = None
  var serverNodeOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientNode = context.actorOf(Client.props(kernel, parameters))
    clientNodeOpt = Some(clientNode)
    val serverNode = context.actorOf(Server.props(kernel, parameters))
    serverNodeOpt = Some(serverNode)
  }

  override def receive: Receive = {
    case message: Message.Single =>
      kernel.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object FullNode extends NodeCompanion[FullNode] {
  def props(kernel: Kernel.Wrap) =
    Props(classOf[FullNode], kernel, Parameters.default)

  def props(kernel: Kernel.Wrap, parameters: Parameters) =
    Props(classOf[FullNode], kernel, parameters)

  implicit val companion: NodeCompanion[FullNode] = this
}
