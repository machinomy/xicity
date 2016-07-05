package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, Props}

class IncomingConnectionBehavior extends Actor with ActorLogging {
  override def receive: Receive = expectConnect orElse expectFailure

  def expectConnect: Receive = {
    case Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      log.info(s"Connected to $endpoint, waiting for Hello")
      context.become(expectHello(endpoint) orElse expectFailure)
  }

  def expectFailure: Receive = {
    case Connection.DidDisconnect() =>
      log.info(s"Disconnected")
      context.stop(self)
    case Connection.DidClose() =>
      log.info(s"Closed")
      context.stop(self)
  }

  def expectHello(endpoint: Endpoint): Receive = {
    case Message.Hello(myAddress, nonce) =>
      log.info(s"Received Hello, sending HelloResponse")
      endpoint.write(Message.HelloResponse(endpoint.address, nonce))
      context.become(expectMessages(endpoint) orElse expectFailure)
  }

  def expectMessages(endpoint: Endpoint): Receive = {
    case something => log.error(s"Got $something")
  }
}

object IncomingConnectionBehavior {
  def props(): Props = Props(classOf[IncomingConnectionBehavior])
}
