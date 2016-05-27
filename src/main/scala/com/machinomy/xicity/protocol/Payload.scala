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

case class PexPayload(ids: Set[Identifier]) extends Payload(PexPayload.name)
object PexPayload extends PayloadCompanion {
  def name = "pex"
}

case class SingleMessagePayload(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Payload(SingleMessagePayload.name)
object SingleMessagePayload extends PayloadCompanion {
  def name = "single"
}

case class RequestPayload(nonce: Long, text: Array[Byte]) extends Payload(RequestPayload.name)
object RequestPayload extends PayloadCompanion {
  def name = "request"
}

case class ResponsePayload(nonce: Long, text: Array[Byte]) extends Payload(ResponsePayload.name)
object ResponsePayload extends PayloadCompanion {
  def name = "response"
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
