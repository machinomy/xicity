package com.machinomy.xicity.examples

import akka.actor.ActorSystem
import com.machinomy.xicity.network.LightNode
import com.machinomy.xicity.transport.{Address, ClientMonitorActor, DefaultBehavior}

object ClientExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val serverAddress = Address.apply("127.0.0.1")
    val client = LightNode(system)
    val startedClient = LightNode.start(client)
    LightNode.stop(startedClient)
  }
}
