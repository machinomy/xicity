package com.machinomy.xicity.transport

import akka.actor.ActorContext
import com.machinomy.xicity.transport.Message.Message

class Messaging {
  def didClientConnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = {
    endpoint.write(Message.Hello(endpoint.address))
  }

  def didRead(message: Message, endpoint: Endpoint)(implicit context: ActorContext): Unit = message match {
    case Message.Pex(ids) =>
      endpoint.write(Message.PexResponse(ids))
    case Message.PexResponse(ids) =>
    case Message.Hello(myAddress, nonce) =>
    case Message.HelloResponse(herAddress, nonce) =>
    case Message.Shot(from, to, text, expiration) =>
  }
}