package com.machinomy.xicity.examples

import akka.actor.ActorSystem
import com.machinomy.xicity.connectivity.{Address, DefaultBehavior, Server}

object ServerExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val address = Address.apply("0.0.0.0")
    system.actorOf(Server.props(address, DefaultBehavior.ServerBehavior()))
  }
}
