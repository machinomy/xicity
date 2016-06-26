package com.machinomy.xicity.connectivity

import akka.actor._
import akka.io.{IO, Tcp}

class Listener(local: Address, handlers: Listener.Handlers) extends Actor with ActorLogging {
  var wireOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    implicit val actorSytem = context.system
    IO(Tcp) ! Tcp.Bind(self, local.address)
  }

  override def receive: Receive = {
    case Tcp.Bound(localAddress) =>
      wireOpt = Some(sender())
      log info s"Bound to $localAddress"
    case Tcp.Connected(remoteAddress, localAddress) =>
      for {
        wire <- wireOpt
        remote = Address(remoteAddress)
        vertex = Endpoint(remote, wire)
        handler <- handlers(vertex)
      } yield {
        log info s"Got incoming connection from $remote"
        wire ! Tcp.Register(handler)
        handler ! Tcp.Connected(remoteAddress, localAddress)
      }
    case Tcp.CommandFailed(cmd: Tcp.Bind) =>
      log error s"Can not bind to ${cmd.localAddress}"
      context stop self
    case Tcp.ErrorClosed(cause) =>
      log error s"Closed connection on error: $cause"
      context stop self
  }

  override def postStop(): Unit = {
    log info "Shutting down the listener..."
    for {
      wire <- wireOpt
    } yield wire ! Tcp.Close
  }
}

object Listener {
  type Handlers = ConnectionFactory[Endpoint]
  def props(local: Address, handlers: Handlers) = Props(classOf[Listener], local, handlers)
}
