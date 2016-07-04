package com.machinomy.xicity.transport

import akka.actor.ActorContext

case class Endpoint(address: Address, wire: Wire) {
  def write[A <: Message.Message](message: A)(implicit context: ActorContext) = wire.write(Message.encode(message))
}
