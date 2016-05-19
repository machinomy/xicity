package com.machinomy.stox.dht

case class RpcPacket[A](payload: A, requestId: RequestId)
