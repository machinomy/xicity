package com.machinomy.xicity.network

import akka.actor.{ActorRef, ActorRefFactory}
import com.machinomy.xicity.transport.{ClientMonitorActor, DefaultBehavior, DefaultParameters}

case class Client(refFactory: ActorRefFactory, clientMonitorOpt: Option[ActorRef] = None)

object Client {
  def apply(refFactory: ActorRefFactory): Client = new Client(refFactory)

  def start(client: Client): Client = client.clientMonitorOpt match {
    case Some(actorRef) =>
      client
    case None =>
      val transportParameters = DefaultParameters
      val seeds = transportParameters.seeds
      val threshold = transportParameters.threshold
      val behavior = DefaultBehavior.ClientMonitorBehavior()
      val props = ClientMonitorActor.props(seeds, threshold, behavior)
      val clientMonitor = client.refFactory.actorOf(props)
      client.copy(clientMonitorOpt = Some(clientMonitor))
  }

  def stop(client: Client): Client = client.clientMonitorOpt match {
    case Some(actorRef) =>
      client.refFactory.stop(actorRef)
      client.copy(clientMonitorOpt = None)
    case None =>
      client
  }
}
