package com.machinomy.xicity.network

import com.machinomy.xicity.Identifier

object Peer {
  sealed trait Callback
  case class IsReady() extends Callback
  case class Received(from: Identifier, text: Array[Byte], expiration: Long) extends Callback
}
