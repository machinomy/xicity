package com.machinomy.xicity.network

import akka.actor._
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.Parameters

class Node[A: NodeCompanion](identifier: Identifier,
                             callback: ActorRef,
                             parameters: Parameters)
  extends Actor with ActorLogging {

  private var kernelActor: ActorRef = _
  private var nodeActor: ActorRef = _

  override def preStart(): Unit = {
    kernelActor = context.actorOf(Kernel.props(identifier, self), s"kernel-${identifier.number}")
    val kernelWrap = Kernel.Wrap(kernelActor, parameters)
    nodeActor = context.actorOf(implicitly[NodeCompanion[A]].props(kernelWrap, parameters), "client-node")
  }

  override def receive: Receive = {
    case e: Node.Callback => callback ! e
    case anything => nodeActor ! anything
  }

  override def postStop(): Unit = {
    context.stop(kernelActor)
    context.stop(nodeActor)
  }
}

object Node {
  sealed trait Callback
  case class IsReady() extends Callback
  case class Received(from: Identifier, protocol: Long, text: Array[Byte], expiration: Long) extends Callback

  def props[A: NodeCompanion](identifier: Identifier, callback: ActorRef, parameters: Parameters) =
    Props(classOf[Node[A]], identifier, callback, parameters)

  def props[A: NodeCompanion](identifier: Identifier, callback: ActorRef) =
    Props(classOf[Node[A]], identifier, callback, Parameters.default)
}
