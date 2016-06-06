package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.io.{IO, Tcp}
import com.machinomy.xicity.connectivity.Connector

class PeerClient(remote: Connector, handler: ActorRef) extends Actor with ActorLogging {
  import context.system

  def receive = {
    case PeerClient.StartCommand =>
      IO(Tcp) ! Tcp.Connect(remote.address)
      log.info(s"Connecting to $remote...")
    case Tcp.CommandFailed(cmd) =>
      log.info(s"Command $cmd failed!")
      context stop self
    case Tcp.Connected(remoteAddress, localAddress) =>
      val connection = sender
      connection ! Tcp.Register(handler)
      log.info(s"OutgoingConnection to $remoteAddress")
      handler ! PeerConnection.OutgoingConnection(connection, Connector(remoteAddress), Connector(localAddress))
    case cmd: PeerClient.SendSingleMessageCommand =>
      handler ! PeerConnection.SingleMessage(cmd.from, cmd.to, cmd.text, cmd.expiration)
    case e =>
      log.warning(e.toString)
  }
}

object PeerClient {
  sealed trait Protocol
  case object StartCommand extends Protocol
  case class SendSingleMessageCommand(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Protocol

  def props(remote: Connector, handler: ActorRef): Props = Props(classOf[PeerClient], remote, handler)
}
