package com.machinomy.stox.dht

import com.machinomy.stox.node.NodeInfo
import com.machinomy.stox.sodium.PublicKey

case class KBuckets(bucketSize: Int, buckets: Map[KBucketIndex, KBucket], baseKey: PublicKey) {
  def allNodes: Seq[NodeInfo] =
    for {
      kBucket <- buckets.values.toSeq
      kBucketEntry <- kBucket.bucketNodes.values
      node = kBucketEntry.entryNode
    } yield node
}

object KBuckets {
  val DEFAULT_BUCKET_SIZE = 8

  def empty(publicKey: PublicKey) = KBuckets(DEFAULT_BUCKET_SIZE, Map.empty, publicKey)
}
