package com.machinomy.xicity.examples

import akka.actor.ActorSystem
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.transport.{DefaultBehavior, DefaultParameters, NodeActor, Parameters}

object ClientNodeExample {
  def run(): Unit = {
    val system = ActorSystem()
    val identifier = Identifier.random
    println(s"GOT IDENTIFIER: $identifier")
    val nodeBehavior = DefaultBehavior.ClientNodeBehavior(identifier, Parameters.default)
    val nodeActor = system.actorOf(NodeActor.props(nodeBehavior))
  }
}
