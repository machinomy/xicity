package com.machinomy.xicity.transport

import java.net.InetSocketAddress

import akka.actor._
import akka.io.{IO, Tcp}

class Server(local: Address, behavior: Server.BehaviorWrap) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    implicit val actorSystem = context.system
    IO(Tcp) ! Tcp.Bind(self, local.address)
    log.info(s"Binding to ${local.address}")
  }

  override def receive: Receive = {
    case Tcp.Bound(localAddress) =>
      log.info(s"Bound to $localAddress")
      behavior.didBound(localAddress)
    case Tcp.Connected(remoteAddress, localAddress) =>
      log.info(s"Connected to $localAddress")
      behavior.didConnect(sender, remoteAddress, localAddress)
    case Tcp.CommandFailed(cmd: Tcp.Bind) =>
      log.error(s"Can not bind to ${cmd.localAddress}")
      context.stop(self)
    case Tcp.ErrorClosed(cause) =>
      log.error(s"Closed connection on error: $cause")
      behavior.didDisconnect()
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.info("Shutting down the listener...")
    behavior.didClose()
  }
}

object Server {
  sealed trait Event
  case class DidBound(localAddress: InetSocketAddress) extends Event
  case class DidConnect(tcpActorRef: ActorRef, remoteAddress: InetSocketAddress, localAddress: InetSocketAddress) extends Event
  case class DidDisconnect() extends Event
  case class DidClose() extends Event

  def props(local: Address, behavior: BehaviorWrap) = Props(classOf[Server], local, behavior)

  case class BehaviorWrap(actorRef: ActorRef) extends ActorWrap {
    def didBound(localAddress: InetSocketAddress)(implicit context: ActorContext) =
      actorRef ! DidBound(localAddress)
    def didConnect(tcpActorRef: ActorRef, remoteAddress: InetSocketAddress, localAddress: InetSocketAddress)(implicit context: ActorContext) =
      actorRef ! DidConnect(tcpActorRef, remoteAddress, localAddress)
    def didDisconnect()(implicit context: ActorContext) =
      actorRef ! DidDisconnect()
    def didClose()(implicit context: ActorContext) =
      actorRef ! DidClose()
  }

  abstract class Behavior extends EventHandler[Event]
}
