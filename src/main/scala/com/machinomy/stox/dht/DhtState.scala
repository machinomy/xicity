package com.machinomy.stox.dht

import com.machinomy.stox.sodium.{Keypair, PublicKey}

case class DhtState(keypair: Keypair, buckets: KBuckets, searchList: Map[PublicKey, DhtSearchEntry])

object DhtState {
  def empty(keypair: Keypair) = DhtState(keypair, KBuckets.empty(keypair.publicKey), Map.empty)
}
