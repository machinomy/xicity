package com.machinomy.stox.node

import akka.actor.{Actor, ActorRef}
import akka.io.{IO, Udp}

class PeerActor(nodeInfo: NodeInfo) extends Actor {
  import context.system

  IO(Udp) ! Udp.SimpleSender

  def receive = {
    case Udp.SimpleSenderReady => {
      sender !
      context.become(ready(sender))
    }
  }

  def ready(out: ActorRef): Receive = {
    case msg: String => println(msg)
  }
}
