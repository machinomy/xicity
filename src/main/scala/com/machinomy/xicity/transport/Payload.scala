package com.machinomy.xicity.transport

import scodec._
import scodec.bits.{BitVector, ByteVector}
import scodec.codecs._

object Payload {
  type Discriminator[A <: Message] = scodec.codecs.Discriminator[Message, A, Byte]

  implicit val requestCodec = new Codec[Request] {
    override def encode(value: Request): Attempt[BitVector] =
      for {
        idBits <- vlongL.encode(value.id)
        textBits <- bytes.encode(ByteVector(value.text))
      } yield idBits ++ textBits
    override def sizeBound: SizeBound = vlongL.sizeBound + bytes.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[Request]] =
      for {
        idR <- vlongL.decode(bits)
        id = idR.value
        textR <- bytes.decode(idR.remainder)
        text = textR.value.toArray
      } yield DecodeResult(Request(id, text), textR.remainder)
  }

  implicit val responseCodec = new Codec[Response] {
    override def encode(value: Response): Attempt[BitVector] =
      for {
        idBits <- vlongL.encode(value.id)
        textBits <- bytes.encode(ByteVector(value.text))
      } yield idBits ++ textBits
    override def sizeBound: SizeBound = vlongL.sizeBound + bytes.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[Response]] =
      for {
        idR <- vlongL.decode(bits)
        id = idR.value
        textR <- bytes.decode(idR.remainder)
        text = textR.value.toArray
      } yield DecodeResult(Response(id, text), textR.remainder)
  }

  implicit val codec: Codec[Message] =
    discriminated[Message].by(byte)
      .typecase(1, implicitly[Codec[Request]])
      .typecase(2, implicitly[Codec[Response]])

  sealed trait Message
  case class Request(id: Long, text: Array[Byte]) extends Message
  case class Response(id: Long, text: Array[Byte]) extends Message
}
