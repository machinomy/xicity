package com.machinomy.xicity.examples

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.{Address, Parameters}
import com.machinomy.xicity.network.{FullNode, Peer, PeerBase}
import com.typesafe.scalalogging.LazyLogging

object SeedApp extends App with LazyLogging {
  class Node(parameters: Parameters) extends Actor with ActorLogging {
    override def preStart(): Unit = {
      logger.info(s"Using $parameters")
      context.actorOf(PeerBase.props[FullNode](identifier, self, parameters), s"peer-${identifier.number}")
    }

    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"I am Ready")
    }
  }

  val system = ActorSystem("xicity")
  val identifier = Identifier.random
  system.actorOf(Props(classOf[Node], effectiveParameters), s"peer-${identifier.number}")

  def effectiveParameters: Parameters = {
    val exclusions = args.map(Address(_)).toSet
    if (exclusions.isEmpty) {
      Parameters.default
    } else {
      val seeds = Parameters.default.seeds -- exclusions
      Parameters.default.copy(seeds = seeds)
    }
  }
}
