package com.machinomy.xicity.transport

import akka.actor.{Actor, ActorLogging}

import scala.reflect.ClassTag

abstract class EventHandler[A](implicit classTag: ClassTag[A]) extends Actor with ActorLogging {
  type Handle = PartialFunction[A, Unit]

  override def receive: Receive = {
    case something => something match {
      case event: A => handle(event)
      case anything => throw new IllegalArgumentException(s"Received unexpected $anything")
    }
  }

  def handle: Handle
}
