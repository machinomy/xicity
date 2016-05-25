package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

class PeerNode(identifier: Identifier) extends Actor with ActorLogging {
  var server: ActorRef = null

  override def receive: Receive = {
    case PeerNode.StartServerCommand(connector) =>
      val handlerFactory = () => context.actorOf(PeerConnection.props)
      server = context.actorOf(PeerServer.props(identifier, connector, handlerFactory))
      server ! PeerServer.StartCommand
    case e: PeerServer.CanNotBind =>
      log.warning(s"Peer Node can not be started: $e")
  }
}

object PeerNode {
  sealed trait Protocol
  case class StartServerCommand(connector: Connector) extends Protocol

  def props(identifier: Identifier) = Props(classOf[PeerNode], identifier)
}
