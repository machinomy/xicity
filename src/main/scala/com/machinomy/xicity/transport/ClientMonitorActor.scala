package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, Props}

import scala.annotation.tailrec
import scala.util.Random

class ClientMonitorActor(seeds: Set[Address], threshold: Byte, initialBehavior: ClientMonitorActor.Behavior) extends Actor {
  assert(threshold >= 0)

  var behavior = initialBehavior

  override def preStart(): Unit = {
    behavior = addClients(selectSeeds(threshold), behavior)
  }

  override def receive = {
    case something => throw new IllegalArgumentException(s"Not planned to receive anything, got: $something")
  }

  def selectSeeds(n: Byte): Set[Address] = Random.shuffle(seeds).take(n)

  @tailrec
  final def addClients(addresses: Set[Address], behavior: ClientMonitorActor.Behavior): ClientMonitorActor.Behavior =
    if (addresses.isEmpty) {
      behavior
    } else {
      addClients(addresses.tail, behavior.addClient(addresses.head))
    }
}

object ClientMonitorActor {
  trait Behavior {
    def addClient(address: Address)(implicit context: ActorContext): Behavior
  }

  def props(seeds: Set[Address], threshold: Byte, behavior: Behavior) = Props(classOf[ClientMonitorActor], seeds, threshold, behavior)
}
