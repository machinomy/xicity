package com.machinomy.xicity.protocol

import com.machinomy.xicity.protocol.JavaPayload.JavaCodec

object WiredPayload {
  val NAME_FIELD_LENGTH = 12

  val nameToCodec: Map[String, JavaCodec[_]] = Map(
    VersionPayload.name -> new JavaPayload.JavaCodec[VersionPayload],
    PexPayload.name -> new JavaPayload.JavaCodec[PexPayload],
    SingleMessagePayload.name -> new JavaPayload.JavaCodec[SingleMessagePayload]
  )

  def fromBytes(bytes: Array[Byte]): Option[JavaPayload] = {
    val (head, tail) = bytes.splitAt(NAME_FIELD_LENGTH)
    val name = head.map(_.toChar).mkString.trim
    val codec = nameToCodec(name)
    codec.fromBytes(tail)
  }

  def toBytes[A <: JavaPayload](payload: A): Array[Byte] = {
    def pad(name: String): String = name.padTo(NAME_FIELD_LENGTH, ' ')
    val nameField = pad(payload.name).getBytes
    val codec = nameToCodec(payload.name)
    val bytes: Array[Byte] = codec.toBytes(payload)
    nameField ++ bytes
  }
}
