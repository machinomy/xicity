package com.machinomy.xicity

import scala.util.Random

case class Identifier(n: BigInt) {
  assert(Identifier.isOk(this))
}

object Identifier {
  val BYTES_LENGTH = 32 // SHA-256

  def isOk(peerIdentifier: Identifier): Boolean = peerIdentifier.n.toByteArray.length <= BYTES_LENGTH

  def random: Identifier = Identifier(math.abs(new Random().nextLong()))

  def distance(a: Identifier, b: Identifier): BigInt = a.n ^ b.n
}
