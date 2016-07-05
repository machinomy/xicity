package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, Props}

import scala.util.Random

class ClientMonitor(seeds: Set[Address], threshold: Byte) extends Actor with ActorLogging {
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
    Client.BehaviorWrap(context.actorOf(ClientBehavior.props()))
}

object ClientMonitor {
  def props(seeds: Set[Address], threshold: Byte) = Props(classOf[ClientMonitor], seeds, threshold)
}
