package com.machinomy.xicity.mac

import akka.actor.ActorContext
import com.machinomy.xicity.encoding.Bytes

case class Endpoint(address: Address, wire: Wire) {
  def write[A <: Message.Message](message: A)(implicit context: ActorContext) = {
    wire.write(Bytes.encode[Message.Message](message))
  }

  override def toString: String = s"Endpoint(${address.address})"
}
