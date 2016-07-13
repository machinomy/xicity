package com.machinomy.xicity.transport

import akka.actor.{ActorContext, ActorRef, Props}
import akka.io.Tcp

class ClientBehavior(kernel: Kernel.Wrap, parameters: Parameters) extends Client.Behavior {
  override def handle: Handle = {
    case Client.DidConnect(endpoint, remoteAddress, localAddress) =>
      log.info(s"Connected to $endpoint")
      val handler = newHandler(endpoint)
      endpoint.wire.tell(Tcp.Register(handler), context.self)
      handler ! Tcp.Connected(remoteAddress, localAddress)
    case Client.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Client.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
  }

  def newHandler(endpoint: Endpoint): ActorRef =
    context.actorOf(Connection.props(endpoint, connectionBehavior))

  def connectionBehavior()(implicit context: ActorContext) =
    Connection.BehaviorWrap(context.actorOf(OutgoingConnectionBehavior.props(kernel, parameters)))
}

object ClientBehavior {
  def props(kernel: Kernel.Wrap, parameters: Parameters) = Props(classOf[ClientBehavior], kernel, parameters)
}
