package com.machinomy.xicity.sodium

import com.machinomy.xicity.Hex

case class SecretKey(bytes: Array[Byte]) extends Key

object SecretKey {
  def apply(string: String): SecretKey = SecretKey(Hex.decode(string))
}
