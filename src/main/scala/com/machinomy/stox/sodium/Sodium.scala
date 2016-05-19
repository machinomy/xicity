package com.machinomy.stox.sodium

import scala.util.Random

object Sodium {
  protected val instance = NaCl.sodium()
  instance.sodium_init()

  val NONCE_BYTES = NaCl.Sodium.NONCE_BYTES
  val ZERO_BYTES = NaCl.Sodium.ZERO_BYTES
  val BOXZERO_BYTES = NaCl.Sodium.BOXZERO_BYTES

  def boxAfterNM(ck: CombinedKey, nonce: Nonce, message: PlainText): Option[CipherText] = {
    val messageBytes = Array.fill[Byte](ZERO_BYTES)(0) ++ message.bytes
    var bytes: Array[Byte] = Array.fill(messageBytes.length)(0)
    val result = instance.crypto_box_afternm(bytes, messageBytes, messageBytes.length.toLong, nonce.bytes, ck.bytes)
    if (result == 0) {
      Some(CipherText(bytes.drop(BOXZERO_BYTES)))
    } else {
      None
    }
  }

  def boxOpenAfterNM(ck: CombinedKey, nonce: Nonce, cipherText: CipherText): Option[PlainText] = {
    val cipherTextBytes = Array.fill[Byte](BOXZERO_BYTES)(0) ++ cipherText.bytes
    var bytes: Array[Byte] = Array.fill(cipherTextBytes.length)(0)
    val result = instance.crypto_box_open_afternm(bytes, cipherTextBytes, cipherTextBytes.length.toLong, nonce.bytes, ck.bytes)
    if (result == 0) {
      Some(PlainText(bytes.drop(ZERO_BYTES)))
    } else {
      None
    }
  }

  def beforeNM(secretKey: SecretKey, publicKey: PublicKey): Option[CombinedKey] = {
    var bytes: Array[Byte] = Array.empty
    val result = instance.crypto_box_beforenm(bytes, publicKey.bytes, secretKey.bytes)
    if (result == 0) {
      Some(CombinedKey(bytes))
    } else {
      None
    }
  }

  def precomputeCombinedKey(secretKey: SecretKey, publicKey: PublicKey) = beforeNM(secretKey, publicKey)

  def newNonce: Nonce = Nonce(Array.fill(Sodium.NONCE_BYTES)(Random.nextInt.toByte))

  def newKeypair: Option[Keypair]= {
    var pkBytes: Array[Byte] = Array.empty
    var skBytes: Array[Byte] = Array.empty
    val result = instance.crypto_box_keypair(pkBytes, skBytes)
    if (result == 0) {
      Some(Keypair(SecretKey(skBytes), PublicKey(pkBytes)))
    } else {
      None
    }
  }
}
