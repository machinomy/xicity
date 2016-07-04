package com.machinomy.xicity

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.transport.Address

class NodeActor(identifier: Identifier) extends Actor with ActorLogging {
  var routingTable = RoutingTable.empty
  var connections = Map.empty[Address, ActorRef]

  override def receive: Receive = ???
}

object NodeActor {
  def props() = Props(classOf[NodeActor])
}
