package com.machinomy.xicity.protocol

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, ObjectInputStream, ObjectOutputStream}

import com.machinomy.xicity.{Connector, Identifier}

import scala.util.{Random, Try}

abstract class Payload(val name: String) extends Serializable

object Payload {
  class JavaCodec[A <: Payload] {
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
