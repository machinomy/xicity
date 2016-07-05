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
        val clientMonitorBehavior = ClientMonitorBehavior(this)
        val seeds = parameters.seeds
        val threshold = parameters.threshold
        val clientMonitorActor = context.actorOf(ClientMonitorActor.props(seeds, threshold, clientMonitorBehavior))
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

  case class ServerBehavior(nodeBehavior: NodeActor.Behavior, localAddressOpt: Option[InetSocketAddress] = None, tmpHandlers: Set[ActorRef] = Set.empty)
    extends Server.Behavior with LazyLogging {

    override def didBound(localAddress: InetSocketAddress) = {
      logger.info(s"Bound to $localAddress")
      copy(localAddressOpt = Some(localAddress))
    }

    override def didConnect(remoteAddress: InetSocketAddress, connection: ActorRef)(implicit context: ActorContext) = {
      val address = Address(remoteAddress)
      val endpoint = Endpoint(address, Wire(connection))
      val handler = newHandler(endpoint)
      connection ! Tcp.Register(handler)
      logger.info(s"Server bound to $localAddressOpt got connection from $remoteAddress")
      nodeBehavior.didIncomingConnection(endpoint)
      copy(tmpHandlers = tmpHandlers + handler)
    }

    override def didDisconnect() = {
      logger.info(s"Server got unbound from $localAddressOpt")
      copy(localAddressOpt = None)
    }

    override def didClose() = {
      logger.info(s"Closed server bound to $localAddressOpt")
      copy(localAddressOpt = None)
    }

    def newHandler(endpoint: Endpoint)(implicit context: ActorContext) =
      context.actorOf(Connection.props(endpoint, connectionBehavior))

    def connectionBehavior()(implicit context: ActorContext) =
      Connection.BehaviorWrap(context.actorOf(IncomingConnectionBehavior.props()))
  }

  case class ClientMonitorBehavior(nodeBehavior: NodeActor.Behavior, clients: Map[Address, ActorRef] = Map.empty)
    extends ClientMonitorActor.Behavior
       with LazyLogging {

    override def addClient(address: Address)(implicit context: ActorContext) = {
      val actor = context.actorOf(Client.props(address, clientActorBehavior))
      logger.info(s"Adding client connected to $address")
      copy(clients = clients.updated(address, actor))
    }

    def clientActorBehavior()(implicit context: ActorContext) =
      Client.BehaviorWrap(context.actorOf(ClientBehavior.props()))
  }
}
