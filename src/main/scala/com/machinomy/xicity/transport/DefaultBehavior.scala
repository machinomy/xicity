package com.machinomy.xicity.transport
import java.net.InetSocketAddress

import akka.actor.{ActorContext, ActorRef}
import akka.io.Tcp
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.transport.NodeActor.Behavior
import com.typesafe.scalalogging.LazyLogging

object DefaultBehavior {

  case class ClientNodeBehavior(identifier: Identifier,
                                parameters: Parameters,
                                selfActorOpt: Option[ActorRef] = None,
                                clientMonitorActorOpt: Option[ActorRef] = None)
    extends NodeActor.Behavior
       with LazyLogging {

    override def start()(implicit context: ActorContext): Behavior = clientMonitorActorOpt match {
      case None =>
        val seeds = parameters.seeds
        val threshold = parameters.threshold
        val clientMonitorActor = context.actorOf(ClientMonitor.props(seeds, threshold))
        copy(clientMonitorActorOpt = Some(clientMonitorActor), selfActorOpt = Some(context.self))
      case Some(actorRef) =>
        this
    }

    override def stop()(implicit context: ActorContext): Behavior = clientMonitorActorOpt match {
      case None =>
        this
      case Some(actorRef) =>
        context.stop(actorRef)
        copy(clientMonitorActorOpt = None, selfActorOpt = None)
    }
  }
}
