package com.machinomy.xicity.network

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac._

// @fixme
class FullNode(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  var clientMonitorActorOpt: Option[ActorRef] = None
  var serverActorOpt: Option[ActorRef] = None
  var serverBehaviorActorOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val clientMonitorProps = ClientMonitor.props(kernel, parameters)
    val clientMonitorActor = context.actorOf(clientMonitorProps)
    clientMonitorActorOpt = Some(clientMonitorActor)

    val serverBehaviorActor = context.actorOf(ServerBehavior.props(kernel, parameters))
    serverBehaviorActorOpt = Some(serverBehaviorActor)
    val serverBehaviorWrap = Server.BehaviorWrap(serverBehaviorActor)
    val serverActor = context.actorOf(Server.props(parameters.serverAddress, serverBehaviorWrap))
    serverActorOpt = Some(serverActor)
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

object FullNode {
  def props(kernel: Kernel.Wrap, parameters: Parameters = Parameters.default) =
    Props(classOf[FullNode], kernel, parameters)
}
