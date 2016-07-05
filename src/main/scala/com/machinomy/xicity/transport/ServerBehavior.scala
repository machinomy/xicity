package com.machinomy.xicity.transport

import java.net.InetSocketAddress

import akka.actor.Props
import akka.io.Tcp

class ServerBehavior(node: Node.Wrap) extends Server.Behavior {
  var localAddressOpt: Option[InetSocketAddress] = None

  override def handle: Handle = {
    case Server.DidBound(localAddress) =>
      localAddressOpt = Some(localAddress)
      log.info(s"Bound to $localAddress")
    case Server.DidConnect(tcpActorRef, remoteAddress, localAddress) =>
      log.info(s"Received connection from $remoteAddress")
      val endpoint = Endpoint(Address(remoteAddress), Wire(tcpActorRef))
      val handler = newHandler(endpoint)
      tcpActorRef ! Tcp.Register(handler)
      handler ! Tcp.Connected(remoteAddress, localAddress)
      log.info(s"Server bound to $localAddressOpt got connection from $remoteAddress")
    case Server.DidDisconnect() =>
      log.info(s"Disconnected")
      localAddressOpt = None
      context.stop(self)
    case Server.DidClose() =>
      log.info(s"Closed")
      localAddressOpt = None
      context.stop(self)
  }

  def newHandler(endpoint: Endpoint) = context.actorOf(Connection.props(endpoint, connectionBehavior()))

  def connectionBehavior() = Connection.BehaviorWrap(context.actorOf(IncomingConnectionBehavior.props(node)))
}

object ServerBehavior {
  def props(node: Node.Wrap) = Props(classOf[ServerBehavior], node)
}
