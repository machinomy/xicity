package com.machinomy.stox.dht

import com.machinomy.stox.sodium.PublicKey

case class Distance(n: Int) {
  def +(other: Distance) = Distance(n + other.n)
}

object Distance {
  def zero = Distance(0)

  def xorDistance(a: PublicKey, b: PublicKey): BigInt = {
    require(a.bytes.length == b.bytes.length)
    BigInt(a.bytes) ^ BigInt(b.bytes)
  }

  def log2(n: BigInt, m: Int = 0): Int = if (n < 2) m else log2(n / 2, m + 1)
}
