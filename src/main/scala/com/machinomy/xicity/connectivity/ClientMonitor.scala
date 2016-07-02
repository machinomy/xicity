package com.machinomy.xicity.connectivity

import akka.actor.{Actor, ActorRef}

import scala.annotation.tailrec
import scala.collection.immutable.IndexedSeq
import scala.util.Random

class ClientMonitor(seeds: Set[Address], threshold: Byte, initialBehavior: ClientMonitor.Behavior) extends Actor {
  assert(threshold >= 0)

  var behavior = initialBehavior

  override def preStart(): Unit = {
    behavior = addClients(selectSeeds(threshold), behavior)
  }

  override def receive = {
    case something => throw new IllegalArgumentException(s"Not planned to receive anything, got: $something")
  }

  def selectSeeds(n: Byte): IndexedSeq[Address] = Random.shuffle(seeds.toIndexedSeq).take(n)

  @tailrec
  final def addClients(addresses: Seq[Address], behavior: ClientMonitor.Behavior): ClientMonitor.Behavior = addresses match {
    case address :: as =>
      val actor = context.actorOf(Client.props(address, behavior.clientBehavior))
      addClients(as, behavior.addClient(actor))
    case Seq() => behavior
  }
}

object ClientMonitor {
  trait Behavior {
    def addClient(actor: ActorRef): Behavior
    def clientBehavior: Client.Behavior
  }
}
