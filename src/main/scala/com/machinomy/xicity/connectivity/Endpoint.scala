package com.machinomy.xicity.connectivity

import akka.actor.ActorRef

case class Endpoint(address: Address, wire: ActorRef)
