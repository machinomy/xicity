package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Random

class OutgoingConnectionBehavior(node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  import context.dispatcher

  var endpointOpt: Option[Endpoint] = None

  val tick = context.system.scheduler.schedule(parameters.tickInitialDelay, parameters.tickInterval, self, OutgoingConnectionBehavior.Tick)

  override def receive: Receive = expectConnect orElse expectFailure

  def expectConnect: Receive = {
    case Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      endpointOpt = Some(endpoint)
      node.didAddConnection(endpoint, Connection.BehaviorWrap(self))
      log.info(s"Connected to $endpoint, saying Hello")
      val nonce = Random.nextInt()
      val helloMessage = Message.Hello(endpoint.address, nonce)
      endpoint.write(helloMessage)
      context.become(expectHelloResponse(nonce) orElse expectFailure)
  }

  def expectFailure: Receive = {
    case Connection.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Connection.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
    case OutgoingConnectionBehavior.Tick =>
      // Do Nothing
    case something => throw new IllegalArgumentException(s"Not expected anything, got $something")
  }

  def expectHelloResponse(nonce: Int): Receive = {
    case Message.HelloResponse(myAddress, theirNonce) =>
      if (theirNonce == nonce) {
        log.info(s"Received valid HelloResponse")
        context.become(expectMessages orElse expectFailure)
      } else {
        throw new IllegalArgumentException(s"Expected HelloResponse.nonce set to $nonce, got $theirNonce")
      }
  }

  def expectMessages: Receive = {
    case OutgoingConnectionBehavior.Tick =>
      endpointOpt match {
        case Some(endpoint) =>
          for (identifiers <- node.getIdentifiers(endpoint)) endpoint.write(Message.Pex(identifiers))
        case None =>
          // Do Nothing
      }
    case Message.Pex(identifiers) =>
      for (endpoint <- endpointOpt) {
        node.didPex(endpoint, identifiers)
        for (identifiers <- node.getIdentifiers(endpoint)) endpoint.write(Message.PexResponse(identifiers))
      }
    case Message.PexResponse(identifiers) =>
      for (endpoint <- endpointOpt) {
        node.didPex(endpoint, identifiers)
      }
    case something => log.error(s"Got $something")
  }

  override def postStop(): Unit =
    for (endpoint <- endpointOpt) {
      node.didRemoveConnection(endpoint)
    }
}

object OutgoingConnectionBehavior {
  object Tick

  def props(node: Node.Wrap, parameters: Parameters) = Props(classOf[OutgoingConnectionBehavior], node, parameters)
}
