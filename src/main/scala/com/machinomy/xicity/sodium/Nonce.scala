package com.machinomy.xicity.sodium

case class Nonce(bytes: Array[Byte]) extends Key {
  assert(bytes.length == Sodium.NONCE_BYTES)
}
