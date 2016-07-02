package com.machinomy.xicity.connectivity

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}

class Listener(local: Address, initialBehavior: Listener.Behavior) extends Actor with ActorLogging {
  var behavior = initialBehavior

  override def preStart(): Unit = {
    implicit val actorSytem = context.system
    IO(Tcp) ! Tcp.Bind(self, local.address)
  }

  override def receive: Receive = {
    case Tcp.Bound(localAddress) =>
      behavior = behavior.didBound(sender())
    case Tcp.Connected(remoteAddress, localAddress) =>
      behavior = behavior.didConnect(remoteAddress)
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

object Listener {
  def props(local: Address, behavior: Behavior) = Props(classOf[Listener], local, behavior)

  trait Behavior {
    def didBound(wire: ActorRef): Behavior
    def didConnect(remoteAddress: InetSocketAddress): Behavior
    def didClose(): Behavior
    def didDisconnect(): Behavior
  }
}
