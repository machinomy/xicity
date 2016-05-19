package com.machinomy.stox.dht

import com.machinomy.stox.sodium._

case class DhtPacket(senderPublicKey: PublicKey, encryptionNonce: Nonce, encryptedPayload: CipherText)

object DhtPacket {
  def encrypt(senderKeypair: Keypair, receiverPublicKey: PublicKey, nonce: Nonce, plainText: PlainText): Option[DhtPacket] =
    for {
      combinedKey <- Sodium.precomputeCombinedKey(senderKeypair.secretKey, receiverPublicKey)
      encryptedPayload <- Box.encrypt(combinedKey, nonce, plainText)
    } yield DhtPacket(senderKeypair.publicKey, nonce, encryptedPayload)
}
