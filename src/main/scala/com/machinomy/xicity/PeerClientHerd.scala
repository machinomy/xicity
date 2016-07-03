package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.transport.Address

import scala.util.Random

class PeerClientHerd(identifier: Identifier, handlerFactory: () => ActorRef, threshold: Int, initialSeeds: Set[Address]) extends Actor with ActorLogging {
  var runningClients: Map[Address, ActorRef] = Map.empty
  var seeds: Set[Address] = initialSeeds

  override def receive: Receive = {
    case PeerClientHerd.StartCommand =>
      val selected = selectSeeds(threshold)
      val node = sender()
      for (connector <- selected) {
        val handler = context.actorOf(PeerConnection.props(node))
        val client = context.actorOf(PeerClient.props(connector, handler))
        client ! PeerClient.StartCommand
        node ! PeerNode.AddRunningClient(connector, client)
        runningClients = runningClients + (connector -> client)
      }
  }

  def selectSeeds(n: Int = 1) = Random.shuffle(seeds.toIndexedSeq).take(n)
}

object PeerClientHerd {
  sealed trait Protocol
  case object StartCommand extends Protocol

  def props(identifier: Identifier, handlerFactory: () => ActorRef, threshold: Int, initialSeeds: Set[Address]): Props =
    Props(classOf[PeerClientHerd], identifier, handlerFactory, threshold, initialSeeds)
}
