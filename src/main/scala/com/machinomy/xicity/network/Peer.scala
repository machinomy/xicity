package com.machinomy.xicity.network

import com.machinomy.xicity.Identifier

class Peer {
  sealed trait Protocol
  case class IsReady() extends Protocol
  case class Receive(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long) extends Protocol
  case class Send(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long) extends Protocol
}
