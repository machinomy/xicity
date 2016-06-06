package com.machinomy.xicity.connectivity

import akka.actor.ActorRef

case class Vertex(endpoint: Endpoint, wire: ActorRef)
