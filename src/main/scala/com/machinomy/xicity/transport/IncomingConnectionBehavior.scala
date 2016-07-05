package com.machinomy.xicity.transport

class IncomingConnectionBehavior extends Connection.Behavior {
  var endpointOpt: Option[Endpoint] = None

  override def handle: Handle = {
    case Connection.DidConnect(endpoint) =>
      log.info(s"Connected to $endpoint")
      endpointOpt = Some(endpoint)
    case Connection.DidDisconnect() =>
      log.info(s"Disconnected...")
      context.stop(self)
    case Connection.DidClose() =>
      log.info(s"Closed...")
      context.stop(self)
    case Connection.DidRead(bytes) =>
      log.info(s"Received $bytes")
  }
}
