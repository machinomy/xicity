package com.machinomy.xicity.network

import akka.actor.ActorRef
import com.machinomy.xicity.mac.Message

object Peer {
  sealed trait Callback
  case class IsReady() extends Callback
  case class Received(message: Message.Meaningful) extends Callback

  case class Wrap(peer: ActorRef) {
    def notifyIsReady() = peer ! IsReady()
    def passDownstream(message: Message.Meaningful) = peer ! Received(message)
  }
}
