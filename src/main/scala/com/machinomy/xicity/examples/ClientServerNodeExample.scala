package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.network.{FullNode, Peer}

object ClientServerNodeExample extends App {
  val system = ActorSystem("xicity")

  class Callback extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"I am Ready")
    }
  }

  val identifier = Identifier.random
  val callback: ActorRef = system.actorOf(Props(classOf[Callback]), s"callback-${identifier.number}")
  system.actorOf(Peer.props[FullNode](identifier, callback), s"peer-${identifier.number}")
}
