package com.machinomy.xicity.network

import akka.actor.ActorRef
import com.machinomy.xicity.Identifier

object Peer {
  sealed trait Callback
  case class IsReady() extends Callback
  case class Received(from: Identifier, text: Array[Byte], expiration: Long) extends Callback

  case class Wrap(peer: ActorRef) {
    def notifyIsReady() = peer ! IsReady()
    def notifyReceived(from: Identifier, text: Array[Byte], expiration: Long) = peer ! Received(from, text, expiration)
  }
}
