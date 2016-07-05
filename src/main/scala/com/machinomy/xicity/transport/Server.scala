package com.machinomy.xicity.transport

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}

class Server(local: Address, initialBehavior: Server.Behavior) extends Actor with ActorLogging {
  var behavior = initialBehavior

  override def preStart(): Unit = {
    implicit val actorSystem = context.system
    IO(Tcp) ! Tcp.Bind(self, local.address)
    log.info(s"Binding to ${local.address}")
  }

  override def receive: Receive = {
    case Tcp.Bound(localAddress) =>
      log.info(s"Bound to $localAddress")
      behavior = behavior.didBound(localAddress)
    case Tcp.Connected(remoteAddress, localAddress) =>
      log.info(s"Connected to $localAddress")
      behavior = behavior.didConnect(remoteAddress, sender)
    case Tcp.CommandFailed(cmd: Tcp.Bind) =>
      log.error(s"Can not bind to ${cmd.localAddress}")
      context.stop(self)
    case Tcp.ErrorClosed(cause) =>
      log.error(s"Closed connection on error: $cause")
      behavior = behavior.didDisconnect()
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.info("Shutting down the listener...")
    behavior.didClose()
  }
}

object Server {
  def props(local: Address, behavior: Behavior) = Props(classOf[Server], local, behavior)

  trait Behavior {
    def didBound(localAddress: InetSocketAddress): Behavior
    def didConnect(remoteAddress: InetSocketAddress, wire: ActorRef)(implicit context: ActorContext): Behavior
    def didClose(): Behavior
    def didDisconnect(): Behavior
  }
}
