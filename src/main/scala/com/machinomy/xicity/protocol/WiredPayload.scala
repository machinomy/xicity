package com.machinomy.xicity.protocol

import com.machinomy.xicity.protocol.Payload.Codec

object WiredPayload {
  val NAME_FIELD_LENGTH = 12

  val nameToCodec: Map[String, Codec[_]] = Map(
    VersionPayload.name -> new Payload.Codec[VersionPayload],
    Pex.name -> new Payload.Codec[Pex]
  )

  def fromBytes(bytes: Array[Byte]): Option[Payload] = {
    val (head, tail) = bytes.splitAt(NAME_FIELD_LENGTH)
    val name = head.map(_.toChar).mkString.trim
    val codec = nameToCodec(name)
    codec.fromBytes(tail)
  }

  def toBytes[A <: Payload](payload: A): Array[Byte] = {
    def pad(name: String): String = name.padTo(NAME_FIELD_LENGTH, ' ')
    val nameField = pad(payload.name).getBytes
    val codec = nameToCodec(payload.name)
    val bytes: Array[Byte] = codec.toBytes(payload)
    nameField ++ bytes
  }
}
