package com.machinomy.xicity.connectivity

import akka.actor.ActorRef

trait ConnectionFactory[A] extends Function[A, Option[ActorRef]]
