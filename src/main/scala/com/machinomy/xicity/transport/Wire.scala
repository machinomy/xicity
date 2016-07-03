package com.machinomy.xicity.transport

import akka.actor.{ActorContext, ActorRef}
import akka.io.Tcp
import akka.util.ByteString

case class Wire(actor: ActorRef) {
  def tell(msg: Any, sender: ActorRef): Unit = actor.tell(msg, sender)
  def tell(msg: Any)(implicit context: ActorContext): Unit = tell(msg, context.self)
  def write(bytes: Array[Byte])(implicit context: ActorContext): Unit = tell(Tcp.Write(ByteString(bytes)))
}
