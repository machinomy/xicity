package com.machinomy.xicity.mac

import java.net.InetSocketAddress

import akka.actor.Props
import akka.io.Tcp
import com.machinomy.xicity.network.Kernel

class ServerBehavior(kernel: Kernel.Wrap, parameters: Parameters) extends ServerMonitor.Behavior {
  var localAddressOpt: Option[InetSocketAddress] = None

  override def handle: Handle = {
    case ServerMonitor.DidBound(localAddress) =>
      localAddressOpt = Some(localAddress)
      log.info(s"Bound to $localAddress")
    case ServerMonitor.DidConnect(tcpActorRef, remoteAddress, localAddress) =>
      log.info(s"Received connection from $remoteAddress")
      val endpoint = Endpoint(Address(remoteAddress), Wire(tcpActorRef))
      val handler = newHandler(endpoint)
      tcpActorRef ! Tcp.Register(handler)
      handler ! Tcp.Connected(remoteAddress, localAddress)
      log.info(s"ServerMonitor bound to $localAddressOpt got connection from $remoteAddress")
    case ServerMonitor.DidDisconnect() =>
      log.info(s"Disconnected")
      localAddressOpt = None
      context.stop(self)
    case ServerMonitor.DidClose() =>
      log.info(s"Closed")
      localAddressOpt = None
      context.stop(self)
  }

  def newHandler(endpoint: Endpoint) = context.actorOf(Connection.props(endpoint, connectionBehavior()))

  def connectionBehavior() = Connection.BehaviorWrap(context.actorOf(IncomingConnectionBehavior.props(kernel, parameters)))
}

object ServerBehavior {
  def props(kernel: Kernel.Wrap, parameters: Parameters) = Props(classOf[ServerBehavior], kernel, parameters)
}
