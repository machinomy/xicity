package com.machinomy.xicity.examples

import akka.actor._
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.network.{FullNode, Peer, PeerBase}

object ClientServerNodeExample extends App {
  val system = ActorSystem("xicity")

  class Peer extends Actor with ActorLogging {
    override def preStart(): Unit = {
      system.actorOf(PeerBase.props[FullNode](identifier, self), s"peer-${identifier.number}")
    }

    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"I am Ready")
    }
  }

  val identifier = Identifier.random
  system.actorOf(Props(classOf[Peer]), s"peer-${identifier.number}")
}
