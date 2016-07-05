package com.machinomy.xicity.transport

import akka.actor.{ActorRef, Props}

class OutgoingConnectionBehavior extends Connection.Behavior {
  var endpointOpt: Option[Endpoint] = None
  var messagingOpt: Option[ActorRef] = None

  override def preStart(): Unit = {
    val messaging = context.actorOf(OutgoingConnectionMessaging.props())
    messagingOpt = Some(messaging)
  }

  override def handle: Handle = {
    case m @ Connection.DidConnect(endpoint, remoteAddress, localAddress) =>
      log.info(s"OutgoingConnectionBehavior: Connected to $endpoint")
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
    case Connection.DidRead(bytes) =>
      log.info(s"Received $bytes")
      Message.decode(bytes) match {
        case Some(message) =>
          forward(message)
        case None =>
          log.error(s"Received ${bytes.length} bytes, can not decode")
      }
  }

  def forward(message: Any) = for (messaging <- messagingOpt) messaging ! message
}

object OutgoingConnectionBehavior {
  def props() = Props(classOf[OutgoingConnectionBehavior])
}
