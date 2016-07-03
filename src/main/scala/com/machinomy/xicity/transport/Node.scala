package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorRefFactory, Props}

class Node(listenerAddress: Address, seeds: Set[Address], threshold: Byte, initialBehavior: Node.Behavior)
  extends Actor {

  var behavior = initialBehavior

  override def preStart() = {
    behavior = behavior.startNode(listenerAddress, seeds, threshold)
  }

  override def receive = {
    case something => throw new IllegalArgumentException(s"Not planned to receive anything, got: $something")
  }

  override def postStop() = {
    behavior = behavior.stopNode()
  }

}

object Node {
  trait Behavior extends ClientMonitor.Behavior with Server.Behavior {
    def startNode(listenerAddress: Address,
                  seeds: Set[Address],
                  threshold: Byte)(implicit refFactory: ActorRefFactory): Behavior
    def stopNode()(implicit context: ActorContext): Behavior
  }

  def props(listenerAddress: Address, seeds: Set[Address], threshold: Byte, initialBehavior: Node.Behavior) =
    Props(classOf[Node], listenerAddress, seeds, threshold, initialBehavior)
}
