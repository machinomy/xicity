package com.machinomy.stox.sodium

import java.util
import com.machinomy.stox.Hex

case class PublicKey(bytes: Array[Byte]) extends Key {
  override def hashCode(): Int = util.Arrays.hashCode(bytes)
}

object PublicKey {
  def apply(string: String): PublicKey = PublicKey(Hex.decode(string))
}
