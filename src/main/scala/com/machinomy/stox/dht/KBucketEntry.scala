package com.machinomy.stox.dht

import com.machinomy.stox.node.NodeInfo
import com.machinomy.stox.sodium.PublicKey

case class KBucketEntry(entryBaseKey: PublicKey, entryNode: NodeInfo)

object KBucketEntry {
  implicit val ordering: Ordering[KBucketEntry] = new Ordering[KBucketEntry] {
    override def compare(a: KBucketEntry, b: KBucketEntry): Int = {
      def length(entry: KBucketEntry) = Distance.xorDistance(entry.entryBaseKey, entry.entryNode.publicKey)
      implicitly[Ordering[BigInt]].compare(length(a), length(b))
    }
  }
}
