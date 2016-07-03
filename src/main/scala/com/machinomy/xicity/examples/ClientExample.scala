package com.machinomy.xicity.examples

import akka.actor.ActorSystem
import com.machinomy.xicity.transport.{Address, ClientMonitorActor$, DefaultBehavior}

object ClientExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val serverAddress = Address.apply("127.0.0.1")
    system.actorOf(ClientMonitorActor.props(Set(serverAddress), 1, DefaultBehavior.ClientMonitorBehavior()))
  }
}
