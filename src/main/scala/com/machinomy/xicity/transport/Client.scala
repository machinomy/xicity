package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}

class Client(address: Address, behavior: Client.BehaviorWrap) extends Actor with ActorLogging {
  override def preStart(): Unit = {
    implicit val actorSystem = context.system
    IO(Tcp) ! Tcp.Connect(address.address)
    log.info(s"Connecting to $address")
  }

  override def receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      val endpoint = Endpoint(address, Wire(sender))
      behavior.didConnect(endpoint)
    case Tcp.CommandFailed(cmd) =>
      log.info(s"Command $cmd failed!")
      behavior.didDisconnect()
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.info("Shutting down the client...")
    behavior.didClose()
  }
}

object Client {
  sealed trait Event
  case class DidConnect(endpoint: Endpoint) extends Event
  case class DidDisconnect() extends Event
  case class DidClose() extends Event

  def props(address: Address, behavior: Client.BehaviorWrap) = Props(classOf[Client], address, behavior)

  case class BehaviorWrap(actorRef: ActorRef) extends ActorWrap {
    def didConnect(endpoint: Endpoint)(implicit context: ActorContext) = actorRef ! DidConnect(endpoint)
    def didDisconnect()(implicit context: ActorContext) = actorRef ! DidDisconnect()
    def didClose()(implicit context: ActorContext) = actorRef ! DidClose()
  }

  abstract class Behavior extends EventHandler[Client.Event]
}
