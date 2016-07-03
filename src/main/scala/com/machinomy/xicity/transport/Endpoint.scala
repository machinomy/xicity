package com.machinomy.xicity.transport

import akka.actor.ActorRef

case class Endpoint(address: Address, wire: ActorRef)
