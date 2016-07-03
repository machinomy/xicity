package com.machinomy.xicity.examples

import akka.actor.ActorSystem
import com.machinomy.xicity.connectivity.{Address, ClientMonitor, DefaultBehavior}

object ClientExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val serverAddress = Address.apply("127.0.0.1")
    system.actorOf(ClientMonitor.props(Set(serverAddress), 1, DefaultBehavior.ClientMonitorBehavior()))
  }
}
