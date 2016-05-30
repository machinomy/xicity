package com.machinomy.xicity.protocol

import scodec.bits.BitVector
import scodec.codecs._
import scodec.{Attempt, Codec, DecodeResult}
import shapeless.Sized

object WiredPayload {
  val NAME_FIELD_LENGTH = 12

  def fromBytes(bytes: Array[Byte]): Option[Payload] = {
    /*val (head, tail) = bytes.splitAt(NAME_FIELD_LENGTH)
    val name = head.map(_.toChar).mkString.trim
    val codec = nameToCodec(name)
    codec.fromBytes(tail)*/
    decode(bytes).toOption.map { decodeResult =>
      decodeResult.value
    }
  }

  def decode(bytes: Array[Byte]): Attempt[DecodeResult[Payload]] = Payload.codec.decode(BitVector(bytes))

  def encode[A <: Payload](payload: A): Attempt[BitVector] = {
    Payload.codec.encode(payload)
  }

  def toBytes[A <: Payload](payload: A): Array[Byte] = {
    /*def pad(name: String): String = name.padTo(NAME_FIELD_LENGTH, ' ')
    val nameField = pad(payload.getClass.toString).getBytes
    val codec = nameToCodec(payload.getClass.toString)
    val bytes: Array[Byte] = codec.toBytes(payload)
    nameField ++ bytes*/
    println("fafafasfasfasfafsf")
    println(payload)
    println(Payload.codec.encode(payload))
    encode(payload).toOption.get.toByteArray
  }
}
