package com.machinomy.xicity.examples

import akka.actor.{ActorContext, ActorSystem}
import com.machinomy.xicity.transport.NodeActor.Behavior
import com.machinomy.xicity.transport._
import com.typesafe.scalalogging.LazyLogging

object ServerExample {
  def run(): Unit = {
    implicit val system = ActorSystem()
    val address = Address.apply("0.0.0.0")
    val nodeBehavior = new Behavior with LazyLogging {
      var connections: Set[Endpoint] = Set.empty
      override def didRead(endpoint: Endpoint, bytes: Array[Byte])(implicit context: ActorContext): Unit = Message.decode(bytes) match {
        case Some(message) => println(s"Received $message")
        case None => println(s"Received ${bytes.length} bytes from $endpoint")
      }
      override def stop()(implicit context: ActorContext): Behavior = ???
      override def didIncomingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit = {
        logger.info(s"Got incoming connection from $endpoint")
        connections = connections + endpoint
      }
      override def didOutgoingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???
      override def didIncomingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???
      override def didOutgoingClose(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???
      override def didOutgoingConnection(endpoint: Endpoint)(implicit context: ActorContext): Unit = ???
      override def didIncomingDisconnect(endpoint: Endpoint)(implicit context: ActorContext): Unit = {
        logger.info(s"Got disconnected from $endpoint")
        connections = connections - endpoint
      }
      override def start()(implicit context: ActorContext): Behavior = ???
    }
    system.actorOf(Server.props(address, DefaultBehavior.ServerBehavior(nodeBehavior)))
  }
}
