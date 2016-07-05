package com.machinomy.xicity.transport

import akka.actor.{ActorRef, Props}

class IncomingConnectionBehavior extends Connection.Behavior {
  var endpointOpt: Option[Endpoint] = None
  var messagingOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val messaging = context.actorOf(IncomingConnectionMessaging.props())
    messagingOpt = Some(messaging)
  }

  override def handle: Handle = {
    case m @ Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      log.info(s"Connected to $endpoint")
      endpointOpt = Some(endpoint)
      forward(m)
    case m @ Connection.DidDisconnect() =>
      log.info(s"Disconnected...")
      forward(m)
      context.stop(self)
    case m @ Connection.DidClose() =>
      log.info(s"Closed...")
      forward(m)
      context.stop(self)
    case message =>
      log.info(s"DidRead: $message")
      forward(message)
  }

  def forward(message: Any) = for (messaging <- messagingOpt) messaging ! message
}

object IncomingConnectionBehavior {
  def props(): Props = Props(classOf[IncomingConnectionBehavior])
}
