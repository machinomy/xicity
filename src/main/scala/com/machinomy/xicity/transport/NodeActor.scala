package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorLogging, Props}

class NodeActor(initialBehavior: NodeActor.Behavior) extends Actor with ActorLogging {
  var behavior = initialBehavior

  override def preStart(): Unit = {
    behavior = behavior.start()
  }

  override def receive: Receive = {
    case something => log.error(s"Not expected anything, got $something")
  }

  override def postStop(): Unit = {
    behavior = behavior.stop()
  }
}

object NodeActor {
  trait Behavior {
    def start()(implicit context: ActorContext): Behavior
    def stop()(implicit context: ActorContext): Behavior
    def didOutgoingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit
    def didOutgoingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit
    def didOutgoingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit
    def didIncomingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit
    def didIncomingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit
    def didIncomingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit
    def didRead(endpoint: Endpoint, bytes: Array[Byte])(implicit context: ActorContext): Unit
  }

  def props(behavior: Behavior) = Props(classOf[NodeActor], behavior)
}
