package com.machinomy.stox.sodium

import com.machinomy.stox.Hex

case class SecretKey(bytes: Array[Byte]) extends Key

object SecretKey {
  def apply(string: String): SecretKey = SecretKey(Hex.decode(string))
}
