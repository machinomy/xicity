package com.machinomy.xicity.connectivity

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import akka.io.{IO, Tcp}

class Client(address: Address, initialBehavior: Client.Behavior) extends Actor with ActorLogging {
  var behavior = initialBehavior

  override def preStart(): Unit = {
    implicit val actorSystem = context.system
    IO(Tcp) ! Tcp.Connect(address.address)
    log.info(s"Connecting to $address")
  }

  override def receive = {
    case Tcp.Connected(remoteAddress, localAddress) =>
      val endpoint = Endpoint(address, sender())
      behavior = behavior.didConnect(endpoint, remoteAddress, localAddress)
    case Tcp.CommandFailed(cmd) =>
      log.info(s"Command $cmd failed!")
      behavior = behavior.didDisconnect()
      context.stop(self)
  }

  override def postStop(): Unit = {
    log.info("Shutting down the client...")
    behavior.didClose()
  }
}

object Client {
  trait Behavior {
    def didConnect(endpoint: Endpoint,
                   remoteAddress: InetSocketAddress,
                   localAddress: InetSocketAddress)(implicit context: ActorContext): Behavior
    def didDisconnect(): Behavior
    def didClose(): Behavior
  }

  def props(address: Address, behavior: Behavior) = Props(classOf[Client], address, behavior)
}
