package com.machinomy.stox.dht

import com.machinomy.stox.sodium.PublicKey

case class KBucketIndex(n: Byte)

object KBucketIndex {
  def apply(a: PublicKey, b: PublicKey): KBucketIndex = {
    val distance = Distance.xorDistance(a, b)
    val logDistance = Distance.log2(distance)
    KBucketIndex((255 - logDistance).toByte)
  }
}
