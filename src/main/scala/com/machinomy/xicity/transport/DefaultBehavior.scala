package com.machinomy.xicity.transport
import java.net.InetSocketAddress

import akka.actor.{ActorContext, ActorRef}
import akka.io.Tcp
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.transport.Message.Hello
import com.machinomy.xicity.transport.NodeActor.Behavior
import com.typesafe.scalalogging.LazyLogging

object DefaultBehavior {

  case class ClientNodeBehavior(identifier: Identifier,
                                parameters: Parameters,
                                selfActorOpt: Option[ActorRef] = None,
                                var routingTable: RoutingTable = RoutingTable.empty,
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

    override def didOutgoingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit = {
      logger.info(s"Made outgoing connection to $endpoint")
      endpoint.write(Message.Hello(endpoint.address))
    }

    override def didRead(endpoint: Endpoint, bytes: Array[Byte])(implicit context: ActorContext): Unit = Message.decode(bytes) match {
      case Some(message) => println(s"Received $message")
      case None =>
        println(s"Received ${bytes.length} bytes from $endpoint")
    }

    override def didIncomingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???

    override def didOutgoingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???

    override def didIncomingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???

    override def didOutgoingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???

    override def didIncomingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???

    override def knownIdentifiers(except: Endpoint): Set[Identifier] = routingTable.identifiers

    override def addIdentifiers(endpoint: Endpoint, identifiers: Set[Identifier]): Unit = {
      routingTable += (endpoint -> identifiers)
    }
  }

  case class ServerBehavior(nodeBehavior: NodeActor.Behavior, localAddressOpt: Option[InetSocketAddress] = None, tmpHandlers: Set[ActorRef] = Set.empty)
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
      nodeBehavior.didIncomingConnection(endpoint)
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

    def connectionBehavior(endpoint: Endpoint) = IncomingConnectionBehavior(nodeBehavior, endpoint)
  }


  case class IncomingConnectionBehavior(nodeBehavior: NodeActor.Behavior, endpoint: Endpoint) extends ConnectionActor.Behavior with LazyLogging {
    override def didRead(bytes: Array[Byte])(implicit context: ActorContext) = Message.decode(bytes) match {
      case Some(message) => message match {
        case Message.Hello(myAddress, nonce) =>
          logger.info(s"Received Hello from $endpoint")
          endpoint.write(Message.HelloResponse(endpoint.address, nonce))
          this
        case Message.Pex(identifiers) =>
          logger.info(s"Received Pex from $endpoint")
          nodeBehavior.addIdentifiers(endpoint, identifiers)
          endpoint.write(Message.PexResponse(nodeBehavior.knownIdentifiers(endpoint)))
          this
      }
      case None =>
        logger.info(s"Received something wrong: $bytes")
        this
    }

    override def didDisconnect()(implicit context: ActorContext) = {
      nodeBehavior.didIncomingDisconnect(endpoint)
      logger.info(s"Peer $endpoint closed connection")
      this
    }

    override def didClose()(implicit context: ActorContext) = {
      nodeBehavior.didIncomingClose(endpoint)
      logger.info(s"Closing connection to $endpoint")
      this
    }

    override def didConnect(endpoint: Endpoint)(implicit context: ActorContext) = this // Is not called when a client have connected to a server.
  }


  case class ClientMonitorBehavior(nodeBehavior: NodeActor.Behavior, clients: Map[Address, ActorRef] = Map.empty)
    extends ClientMonitorActor.Behavior
       with LazyLogging {

    override def addClient(address: Address)(implicit context: ActorContext) = {
      val actor = context.actorOf(ClientActor.props(address, clientActorBehavior))
      logger.info(s"Adding client connected to $address")
      copy(clients = clients.updated(address, actor))
    }

    def clientActorBehavior = ClientActorBehavior(nodeBehavior)
  }


  case class ClientActorBehavior(nodeBehavior: NodeActor.Behavior, endpointOpt: Option[Endpoint] = None)
    extends ClientActor.Behavior
       with LazyLogging {

    override def didConnect(endpoint: Endpoint,
                            remoteAddress: InetSocketAddress,
                            localAddress: InetSocketAddress)(implicit context: ActorContext) = {
      logger.info(s"Connected to $endpoint via $remoteAddress on $localAddress")
      endpoint.wire.tell(Tcp.Register(newHandler(endpoint)), context.self)
      nodeBehavior.didOutgoingConnection(endpoint)
      copy(endpointOpt = Some(endpoint))
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

    def connectionBehavior = OutgoingConnectionBehavior(nodeBehavior)
  }

  case class OutgoingConnectionBehavior(nodeBehavior: NodeActor.Behavior, endpointOpt: Option[Endpoint] = None)
    extends ConnectionActor.Behavior with LazyLogging {

    override def didConnect(endpoint: Endpoint)(implicit context: ActorContext) = {
      logger.info(s"Connected to $endpoint")
      nodeBehavior.didOutgoingConnection(endpoint)
      copy(endpointOpt = Some(endpoint))
    }

    override def didRead(bytes: Array[Byte])(implicit context: ActorContext) = {
      for (endpoint <- endpointOpt) {
        nodeBehavior.didRead(endpoint, bytes)
        logger.info(s"Received ${bytes.length} bytes from $endpoint")
      }
      this
    }

    override def didDisconnect()(implicit context: ActorContext) = {
      for (endpoint <- endpointOpt) {
        nodeBehavior.didOutgoingDisconnect(endpoint)
        logger.info(s"Peer $endpoint closed connection")
      }
      this
    }

    override def didClose()(implicit context: ActorContext)= {
      for (endpoint <- endpointOpt) {
        nodeBehavior.didOutgoingClose(endpoint)
        logger.info(s"Closing connection to $endpoint")
      }
      this
    }
  }

}
