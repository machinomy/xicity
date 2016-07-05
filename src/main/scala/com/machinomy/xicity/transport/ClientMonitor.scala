package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, Props}

import scala.util.Random

class ClientMonitor(node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  val seeds = parameters.seeds
  val threshold = parameters.threshold

  assert(threshold >= 0)

  override def preStart(): Unit = {
    log.info(s"Started ClientMonitor")
    addClients(selectSeeds(threshold))
  }

  override def receive = {
    case something => throw new IllegalArgumentException(s"Not planned to receive anything, got: $something")
  }

  def selectSeeds(n: Byte): Set[Address] = Random.shuffle(seeds).take(n)

  def addClients(addresses: Set[Address]): Unit =
    for (address <- addresses) {
      log.info(s"Starting client for $addresses...")
      context.actorOf(Client.props(address, clientBehavior))
    }

  def clientBehavior()(implicit context: ActorContext) =
    Client.BehaviorWrap(context.actorOf(ClientBehavior.props(node, parameters)))
}

object ClientMonitor {
  def props(node: Node.Wrap, parameters: Parameters) = Props(classOf[ClientMonitor], node, parameters)
}
