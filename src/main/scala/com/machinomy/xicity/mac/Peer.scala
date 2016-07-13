package com.machinomy.xicity.mac

import com.machinomy.xicity.Identifier

object Peer {
  sealed trait Callback
  case class IsReady() extends Callback
  case class Received(from: Identifier, protocol: Long, text: Array[Byte], expiration: Long) extends Callback
}
