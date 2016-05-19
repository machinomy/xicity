package com.machinomy.stox.dht

import com.machinomy.stox.node.NodeInfo
import com.machinomy.stox.sodium.PublicKey

case class DhtSearchEntry(searchNode: Option[NodeInfo], searchBuckets: KBuckets)

object DhtSearchEntry {
  def empty(publicKey: PublicKey) = DhtSearchEntry(None, KBuckets.empty(publicKey))
}
