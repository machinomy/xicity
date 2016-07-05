package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, Props}

class IncomingConnectionBehavior(node: Node.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  import context.dispatcher

  var endpointOpt: Option[Endpoint] = None

  override def receive: Receive = expectConnect orElse expectFailure

  def expectConnect: Receive = {
    case Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      endpointOpt = Some(endpoint)
      node.didAddConnection(endpoint, Connection.BehaviorWrap(self))
      log.info(s"Connected to $endpoint, waiting for Hello")
      context.become(expectHello orElse expectFailure)
  }

  def expectFailure: Receive = {
    case Connection.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Connection.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
    case something => throw new IllegalArgumentException(s"Not expected anything, got $something")
  }

  def expectHello: Receive = {
    case Message.Hello(myAddress, nonce) =>
      for (endpoint <- endpointOpt) {
        log.info(s"Received Hello, sending HelloResponse")
        endpoint.write(Message.HelloResponse(endpoint.address, nonce))
        context.become(expectMessages orElse expectFailure)
      }
  }

  def expectMessages: Receive = {
    case Message.Pex(identifiers) =>
      for (endpoint <- endpointOpt) {
        node.didPex(endpoint, identifiers)
        for (identifiers <- node.getIdentifiers(endpoint)) endpoint.write(Message.PexResponse(identifiers))
      }
    case Message.PexResponse(identifiers) =>
      for (endpoint <- endpointOpt) {
        node.didPex(endpoint, identifiers)
      }
    case Connection.DoWrite(message) =>
      for (endpoint <- endpointOpt) {
        endpoint.write(message)
      }
    case message: Message.Shot =>
      node.didReceive(message.from, message.to, message.protocol, message.text, message.expiration)
    case something => throw new IllegalArgumentException(s"Not expected anything, got $something")
  }

  override def postStop(): Unit =
    for (endpoint <- endpointOpt) {
      node.didRemoveConnection(endpoint)
    }

}

object IncomingConnectionBehavior {
  def props(node: Node.Wrap, parameters: Parameters): Props = Props(classOf[IncomingConnectionBehavior], node, parameters)
}
