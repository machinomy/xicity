package com.machinomy.xicity.connectivity
import java.net.InetSocketAddress

import akka.actor.{ActorContext, ActorRef}
import akka.io.Tcp
import com.typesafe.scalalogging.LazyLogging

object DefaultBehavior {

  case class ServerBehavior(localAddressOpt: Option[InetSocketAddress] = None, tmpHandlers: Set[ActorRef] = Set.empty)
    extends Server.Behavior with LazyLogging {

    override def didBound(localAddress: InetSocketAddress) = {
      logger.info(s"Bound to $localAddress")
      copy(localAddressOpt = Some(localAddress))
    }

    override def didConnect(remoteAddress: InetSocketAddress, wire: ActorRef)(implicit context: ActorContext) = {
      val address = Address(remoteAddress)
      val endpoint = Endpoint(address, wire)
      val handler = newHandler(endpoint)
      wire ! Tcp.Register(handler)
      logger.info(s"Server bound to $localAddressOpt got connection from $remoteAddress")
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
      context.actorOf(Connection.props(endpoint, connectionBehavior(endpoint)))

    def connectionBehavior(endpoint: Endpoint) = IncomingConnectionBehavior(endpoint)
  }


  case class IncomingConnectionBehavior(endpoint: Endpoint) extends Connection.Behavior with LazyLogging {
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
    extends ClientMonitor.Behavior
       with LazyLogging {

    override def addClient(address: Address)(implicit context: ActorContext) = {
      val actor = context.actorOf(Client.props(address, clientBehavior))
      logger.info(s"Adding client connected to $address")
      copy(clients.updated(address, actor))
    }

    def clientBehavior = ClientBehavior(this)
  }


  case class ClientBehavior(clientMonitorBehavior: ClientMonitorBehavior, endpointOpt: Option[Endpoint] = None)
    extends Client.Behavior
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
      context.actorOf(Connection.props(endpoint, connectionBehavior))

    def connectionBehavior = OutgoingConnectionBehavior()
  }

  case class OutgoingConnectionBehavior(endpointOpt: Option[Endpoint] = None)
    extends Connection.Behavior with LazyLogging {

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
