package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.Parameters
import com.machinomy.xicity.network.{Peer, PeerBase, Server}

object ServerApp extends App {
  class Dummy extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
    }
  }

  val system = ActorSystem("xicity")
  val identifier = Identifier.random
  val dummy = system.actorOf(Props(classOf[Dummy]))
  val serverParameters = Parameters.default.copy(seeds = Set.empty)
  val peerProps = PeerBase.props[Server](identifier, dummy, serverParameters)
  system.actorOf(peerProps)
}
