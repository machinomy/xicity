package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorContext, ActorRefFactory, Props}

class NodeActor(listenerAddress: Address, seeds: Set[Address], threshold: Byte, initialBehavior: NodeActor.Behavior)
  extends Actor {

  var behavior = initialBehavior

  override def preStart() = {
    behavior = behavior.didStartNode(listenerAddress, seeds, threshold)
  }

  override def receive = {
    case something => throw new IllegalArgumentException(s"Not planned to receive anything, got: $something")
  }

  override def postStop() = {
    behavior = behavior.didStopNode()
  }

}

object NodeActor {
  trait Behavior extends ClientMonitorActor.Behavior with ServerActor.Behavior {
    def didStartNode(listenerAddress: Address,
                     seeds: Set[Address],
                     threshold: Byte)(implicit refFactory: ActorRefFactory): Behavior
    def didStopNode()(implicit context: ActorContext): Behavior
  }

  def props(listenerAddress: Address, seeds: Set[Address], threshold: Byte, initialBehavior: NodeActor.Behavior) =
    Props(classOf[NodeActor], listenerAddress, seeds, threshold, initialBehavior)
}
