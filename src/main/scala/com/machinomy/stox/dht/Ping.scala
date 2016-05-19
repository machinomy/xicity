package com.machinomy.stox.dht

object Ping {
  sealed trait Packet {
    def flag: Boolean
  }
  object PingRequest {
    val flag = false
  }
  object PingResponse {
    val flag = true
  }
}
