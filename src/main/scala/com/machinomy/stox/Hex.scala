package com.machinomy.stox

object Hex {
  def decode(hex: String): Array[Byte] =
    hex.replaceAll("[^0-9A-Fa-f]", "").sliding(2, 2).toArray.map(Integer.parseInt(_, 16).toByte)

  def encode(bytes: Array[Byte]): String = bytes.map("%02x".format(_)).mkString
}
