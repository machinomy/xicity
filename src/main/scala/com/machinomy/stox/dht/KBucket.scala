package com.machinomy.stox.dht

import com.machinomy.stox.sodium.PublicKey

case class KBucket(bucketNodes: Map[PublicKey, KBucketEntry]) {
  def isEmpty: Boolean = bucketNodes.isEmpty
}

object KBucket {
  def empty: KBucket = KBucket(Map.empty)
}
