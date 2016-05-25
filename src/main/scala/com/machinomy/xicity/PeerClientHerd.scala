package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}

import scala.util.Random

class PeerClientHerd(identifier: Identifier, threshold: Int, initialSeeds: Set[Connector]) extends Actor with ActorLogging {
  var runningClients: Map[Connector, ActorRef] = Map.empty
  var seeds: Set[Connector] = initialSeeds

  override def receive: Receive = {
    case PeerClientHerd.StartCommand =>
      val selected = selectSeeds(threshold)
      for (connector <- selected) {
        val handler = context.actorOf(PeerConnection.props(sender))
        val client = context.actorOf(PeerClient.props(connector, handler))
        client ! PeerClient.StartCommand
        runningClients = runningClients.updated(connector, client)
      }
  }

  def selectSeeds(n: Int = 1) = Random.shuffle(seeds.toIndexedSeq).take(n)
}

object PeerClientHerd {
  sealed trait Protocol
  case object StartCommand extends Protocol

  def props(identifier: Identifier, threshold: Int, initialSeeds: Set[Connector]): Props =
    Props(classOf[PeerClientHerd], identifier, threshold, initialSeeds)
}
