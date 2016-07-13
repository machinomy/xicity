package com.machinomy.xicity.mac

import akka.actor.{Actor, ActorRef}

trait ActorWrap {
  def actorRef: ActorRef
  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = actorRef ! message
  def tell(msg: Any, sender: ActorRef): Unit = this.!(msg)(sender)
}
