package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.network.{Client, Peer, PeerBase}

object ClientApp extends App {
  class Dummy extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
      case something =>
        log.info(s"RECEIVED $something")
    }
  }

  val system = ActorSystem("xicity")
  val identifier = Identifier.random
  val dummy = system.actorOf(Props(classOf[Dummy]), "dummy")
  val peerProps = PeerBase.props[Client](identifier, dummy)
  system.actorOf(peerProps, "peer")
}
