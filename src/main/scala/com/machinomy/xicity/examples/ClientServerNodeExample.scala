package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.network.{FullNode, Peer, PeerBase}

object ClientServerNodeExample extends App {
  val system = ActorSystem("xicity")

  class Peer extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"I am Ready")
    }
  }

  val identifier = Identifier.random
  val callback: ActorRef = system.actorOf(Props(classOf[Peer]), s"callback-${identifier.number}")
  system.actorOf(PeerBase.props[FullNode](identifier, callback), s"peer-${identifier.number}")
}
