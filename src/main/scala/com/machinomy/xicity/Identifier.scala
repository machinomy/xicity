package com.machinomy.xicity

import scala.util.Random

case class Identifier(number: BigInt) {
  assert(Identifier.isOk(this))
}

object Identifier {
  val BYTES_LENGTH = 32 // SHA-256

  def isOk(peerIdentifier: Identifier): Boolean = peerIdentifier.number.toByteArray.length <= BYTES_LENGTH

  def random: Identifier = Identifier(math.abs(new Random().nextLong()))

  def apply(hex: String): Identifier = {
    def hex2bytes(hex: String): Array[Byte] =
      hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

    assert(hex.length % 2 == 0)
    Identifier(BigInt(hex2bytes(hex)))
  }

  def distance(a: Identifier, b: Identifier): BigInt = a.number ^ b.number
}
