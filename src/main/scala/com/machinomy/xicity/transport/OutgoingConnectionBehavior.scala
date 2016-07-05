package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging, Props}

import scala.util.Random

class OutgoingConnectionBehavior extends Actor with ActorLogging {
  override def receive: Receive = expectConnect orElse expectFailure

  def expectConnect: Receive = {
    case Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      log.info(s"Connected to $endpoint, saying Hello")
      val nonce = Random.nextInt()
      val helloMessage = Message.Hello(endpoint.address, nonce)
      endpoint.write(helloMessage)
      context.become(expectHelloResponse(nonce, endpoint) orElse expectFailure)
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

  def expectHelloResponse(nonce: Int, endpoint: Endpoint): Receive = {
    case Message.HelloResponse(myAddress, theirNonce) =>
      if (theirNonce == nonce) {
        log.info(s"Received valid HelloResponse")
        context.become(expectMessages(endpoint) orElse expectFailure)
      } else {
        throw new IllegalArgumentException(s"Expected HelloResponse.nonce set to $nonce, got $theirNonce")
      }
  }

  def expectMessages(endpoint: Endpoint): Receive = {
    case something => log.error(s"Got $something")
  }
}

object OutgoingConnectionBehavior {
  def props() = Props(classOf[OutgoingConnectionBehavior])
}
