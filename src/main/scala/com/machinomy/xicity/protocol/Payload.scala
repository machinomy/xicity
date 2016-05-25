package com.machinomy.xicity.protocol

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.machinomy.xicity.{Connector, Identifier}

import scala.util.{Random, Try}

sealed abstract class Payload(val name: String) extends Serializable

case class VersionPayload(remoteConnector: Connector,
                          nonce: Long,
                          userAgent: String) extends Payload(VersionPayload.name)

object VersionPayload extends PayloadCompanion {
  val name = "version"
  def apply(remoteConnector: Connector): VersionPayload = new VersionPayload(remoteConnector, new Random().nextLong(), "xicity/0.1")
}

case class Pex(ids: Set[Identifier]) extends Payload(Pex.name)
object Pex extends PayloadCompanion {
  def name = "pex"
}

object Payload {
  class Codec[A <: Payload] {
    def fromBytes[A](bytes: Array[Byte]) = {
      val byteArrayInputStream = new ByteArrayInputStream(bytes)
      val objectInputStream = new ObjectInputStream(byteArrayInputStream)
      val payload: Option[A] = Try(objectInputStream.readObject().asInstanceOf[A]).toOption
      objectInputStream.close()
      byteArrayInputStream.close()
      payload
    }
    def toBytes[A](payload: A): Array[Byte] = {
      val byteArrayOutputStream = new ByteArrayOutputStream()
      val objectOutputStream = new ObjectOutputStream(byteArrayOutputStream)
      objectOutputStream.writeObject(payload)
      val bytes = byteArrayOutputStream.toByteArray
      objectOutputStream.close()
      byteArrayOutputStream.close()
      bytes
    }
  }
}
