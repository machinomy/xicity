package com.machinomy.xicity

import akka.actor.{ActorRef, ActorRefFactory}
import com.machinomy.xicity.transport.Message.Shot
import com.machinomy.xicity.transport.NodeActor

case class Node(identifier: Identifier,
                nodeActor: Option[ActorRef],
                didStart: Node.DidStart,
                didReceive: Node.DidReceive)

object Node {
  type DidStart = () => Unit
  type DidReceive = Shot => Unit

  def apply(identifier: Identifier): Node = new Node(identifier, None, () => (), shot => ())

  def start(node: Node)(implicit actorRefFactory: ActorRefFactory): Node = node.nodeActor match {
    case Some(actorRef) =>
      node
    case None =>
      //val nodeActor = actorRefFactory.actorOf(NodeActor.props())
      //node.copy(nodeActor = Some(nodeActor))
      node
  }

  def stop(node: Node)(implicit actorRefFactory: ActorRefFactory): Node = node.nodeActor match {
    case Some(actorRef) =>
      actorRefFactory.stop(actorRef)
      node.copy(nodeActor = None)
    case None =>
      node
  }

}
