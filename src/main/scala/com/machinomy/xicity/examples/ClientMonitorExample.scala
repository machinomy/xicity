package com.machinomy.xicity.examples

import akka.actor.{ActorContext, ActorSystem}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.transport.NodeActor.Behavior
import com.machinomy.xicity.transport._

object ClientMonitorExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val serverAddress = Address.apply("127.0.0.1")
    val nodeBehavior: NodeActor.Behavior = new Behavior {
      override def didRead(endpoint: Endpoint, bytes: Array[Byte])(implicit context: ActorContext): Unit = {}
      override def stop()(implicit context: ActorContext): Behavior = this
      override def didIncomingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit = {}
      override def didOutgoingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = {}
      override def didIncomingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit = {}
      override def didOutgoingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit = {}
      override def didOutgoingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit = {}
      override def didIncomingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = {}
      override def start()(implicit context: ActorContext): Behavior = this
    }
    val clientMonitorBehavior = DefaultBehavior.ClientMonitorBehavior(nodeBehavior)
    system.actorOf(ClientMonitorActor.props(Set(serverAddress), 1, clientMonitorBehavior))
  }
}
