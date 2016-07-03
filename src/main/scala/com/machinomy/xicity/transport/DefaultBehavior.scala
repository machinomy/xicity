package com.machinomy.xicity.transport
import java.net.InetSocketAddress

import akka.actor.{ActorContext, ActorRef}
import akka.io.Tcp
import com.typesafe.scalalogging.LazyLogging

object DefaultBehavior {

  case class ServerBehavior(localAddressOpt: Option[InetSocketAddress] = None, tmpHandlers: Set[ActorRef] = Set.empty)
    extends ServerActor.Behavior with LazyLogging {

    override def didBound(localAddress: InetSocketAddress) = {
      logger.info(s"Bound to $localAddress")
      copy(localAddressOpt = Some(localAddress))
    }

    override def didConnect(remoteAddress: InetSocketAddress, connection: ActorRef)(implicit context: ActorContext) = {
      val address = Address(remoteAddress)
      val endpoint = Endpoint(address, Wire(connection))
      val handler = newHandler(endpoint)
      connection ! Tcp.Register(handler)
      logger.info(s"ServerActor bound to $localAddressOpt got connection from $remoteAddress")
      copy(tmpHandlers = tmpHandlers + handler)
    }

    override def didDisconnect() = {
      logger.info(s"ServerActor got unbound from $localAddressOpt")
      copy(localAddressOpt = None)
    }

    override def didClose() = {
      logger.info(s"Closed server bound to $localAddressOpt")
      copy(localAddressOpt = None)
    }

    def newHandler(endpoint: Endpoint)(implicit context: ActorContext) =
      context.actorOf(ConnectionActor.props(endpoint, connectionBehavior(endpoint)))

    def connectionBehavior(endpoint: Endpoint) = IncomingConnectionBehavior(endpoint)
  }


  case class IncomingConnectionBehavior(endpoint: Endpoint) extends ConnectionActor.Behavior with LazyLogging {
    override def didRead(bytes: Array[Byte]) = {
      logger.info(s"Received ${bytes.length} bytes from $endpoint")
      this
    }

    override def didDisconnect() = {
      logger.info(s"Peer $endpoint closed connection")
      this
    }

    override def didClose() = {
      logger.info(s"Closing connection to $endpoint")
      this
    }

    override def didConnect(endpoint: Endpoint) = this
  }


  case class ClientMonitorBehavior(clients: Map[Address, ActorRef] = Map.empty)
    extends ClientMonitorActor.Behavior
       with LazyLogging {

    override def addClient(address: Address)(implicit context: ActorContext) = {
      val actor = context.actorOf(ClientActor.props(address, clientActorBehavior))
      logger.info(s"Adding client connected to $address")
      copy(clients.updated(address, actor))
    }

    def clientActorBehavior = ClientActorBehavior(this)
  }


  case class ClientActorBehavior(clientMonitorBehavior: ClientMonitorBehavior, endpointOpt: Option[Endpoint] = None)
    extends ClientActor.Behavior
       with LazyLogging {

    override def didConnect(endpoint: Endpoint,
                            remoteAddress: InetSocketAddress,
                            localAddress: InetSocketAddress)(implicit context: ActorContext) = {
      logger.info(s"Connected to $endpoint via $remoteAddress on $localAddress")
      endpoint.wire.tell(Tcp.Register(newHandler(endpoint)), context.self)
      this
    }

    override def didDisconnect() = {
      for (endpoint <- endpointOpt) {
        logger.info(s"Got disconnected from $endpoint")
      }
      this
    }

    override def didClose() = {
      for (endpoint <- endpointOpt) {
        logger.info(s"Closing connection to $endpoint")
      }
      this
    }

    def newHandler(endpoint: Endpoint)(implicit context: ActorContext) =
      context.actorOf(ConnectionActor.props(endpoint, connectionBehavior))

    def connectionBehavior = OutgoingConnectionBehavior()
  }

  case class OutgoingConnectionBehavior(endpointOpt: Option[Endpoint] = None)
    extends ConnectionActor.Behavior with LazyLogging {

    override def didConnect(endpoint: Endpoint) = {
      logger.info(s"Connected to $endpoint")
      copy(endpointOpt = Some(endpoint))
    }

    override def didRead(bytes: Array[Byte]) = {
      for (endpoint <- endpointOpt) {
        logger.info(s"Received ${bytes.length} bytes from $endpoint")
      }
      this
    }

    override def didDisconnect() = {
      for (endpoint <- endpointOpt) {
        logger.info(s"Peer $endpoint closed connection")
      }
      this
    }

    override def didClose() = {
      for (endpoint <- endpointOpt) {
        logger.info(s"Closing connection to $endpoint")
      }
      this
    }
  }

}
