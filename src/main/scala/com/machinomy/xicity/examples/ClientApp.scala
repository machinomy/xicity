package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.{Address, Parameters}
import com.machinomy.xicity.network.{Client, Peer, PeerBase}

object ClientApp extends App {
  class Dummy extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
    }
  }

  val system = ActorSystem("xicity")
  val identifier = Identifier.random
  val dummy = system.actorOf(Props(classOf[Dummy]), "dummy")
  val clientParameters = Parameters.default.copy(seeds = Set(Address("0.0.0.0")))
  val peerProps = PeerBase.props[Client](identifier, dummy, clientParameters)
  system.actorOf(peerProps, "peer")
}
