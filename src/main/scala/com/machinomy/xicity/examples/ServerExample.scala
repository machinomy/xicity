package com.machinomy.xicity.examples

import akka.actor.ActorSystem
import com.machinomy.xicity.transport.{Address, DefaultBehavior, ServerActor}

object ServerExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val address = Address.apply("0.0.0.0")
    system.actorOf(ServerActor.props(address, DefaultBehavior.ServerBehavior()))
  }
}
