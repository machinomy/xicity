package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.Parameters
import com.machinomy.xicity.network._

object MacClientExample extends App {
  implicit val system = ActorSystem()

  class Peer extends Actor with ActorLogging {
    override def receive: Receive = {
      case Node.IsReady() =>
        log.info(s"I am Ready")
    }
  }

  val identifier = Identifier.random
  val peer = system.actorOf(Props(classOf[Peer]), "peer")
  val kernel = system.actorOf(Kernel.props(identifier, peer), "kernel")
  val parameters = Parameters.default
  val clientNode = system.actorOf(Client.props(Kernel.Wrap(kernel, parameters), parameters), "client-node")
}
