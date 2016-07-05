package com.machinomy.xicity.transport

import java.net.InetAddress

import com.machinomy.xicity.Identifier
import scodec._
import scodec.bits.{ByteVector, _}
import scodec.codecs._

import scala.language.implicitConversions
import scala.util.Random

object Message {
  type Discriminator[A <: Message] = scodec.codecs.Discriminator[Message, A, Byte]
  implicit val inetAddressCodec = new Codec[InetAddress] {
    val ipv4pad: ByteVector = hex"00 00 00 00 00 00 00 00 00 00 FF FF"
    override def encode(value: InetAddress): Attempt[BitVector] = {
      val inetAddressBytes = ByteVector(value.getAddress)
      if (inetAddressBytes.length == 4) {
        bytes(16).encode(ipv4pad ++ inetAddressBytes)
      } else {
        bytes(16).encode(inetAddressBytes)
      }
    }
    override def decode(bits: BitVector): Attempt[DecodeResult[InetAddress]] = {
      bytes(16).decode(bits).map { b =>
        val bts = if (b.value.take(12) == ipv4pad) b.value.drop(12) else b.value
        DecodeResult(InetAddress.getByAddress(bts.toArray), b.remainder)
      }
    }
    override def sizeBound: SizeBound = SizeBound.exact(16)
  }
  implicit val addressCodec: Codec[Address] = new Codec[Address] {
    val portCodec = uint16L
    override def encode(value: Address): Attempt[BitVector] =
      for {
        address <- inetAddressCodec.encode(value.address.getAddress)
        port <- portCodec.encode(value.address.getPort)
      } yield address ++ port
    override def sizeBound: SizeBound = inetAddressCodec.sizeBound + portCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[Address]] =
      for {
        inetAddressR <- inetAddressCodec.decode(bits)
        portR <- portCodec.decode(inetAddressR.remainder)
      } yield DecodeResult(Address(inetAddressR.value, portR.value), portR.remainder)
  }
  implicit val identifierCodec: Codec[Identifier] = new Codec[Identifier] {
    val bytesCodec = bytes(Identifier.BYTES_LENGTH)
    override def encode(value: Identifier): Attempt[BitVector] =
      for {
        bytes <- bytesCodec.encode(ByteVector(value.n.toByteArray).padLeft(Identifier.BYTES_LENGTH))
      } yield bytes

    override def sizeBound: SizeBound = bytesCodec.sizeBound

    override def decode(bits: BitVector): Attempt[DecodeResult[Identifier]] =
      for {
        bigIntBytesR <- bytesCodec.decode(bits)
        bigIntBytes = bigIntBytesR.value.toArray
      } yield DecodeResult(Identifier(BigInt(bigIntBytes)), bigIntBytesR.remainder)
  }
  implicit val helloCodec = new Codec[Hello] {
    val nonceCodec = int32L
    val userAgentCodec = ascii32
    override def encode(value: Hello): Attempt[BitVector] =
      for {
        addressBits <- addressCodec.encode(value.remoteAddress)
        nonceBits <- nonceCodec.encode(value.nonce)
      } yield addressBits ++ nonceBits
    override def sizeBound: SizeBound = addressCodec.sizeBound + nonceCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[Hello]] =
      for {
        addressR <- addressCodec.decode(bits)
        address = addressR.value
        nonceR <- nonceCodec.decode(addressR.remainder)
        nonce = nonceR.value
      } yield DecodeResult(Hello(address, nonce), nonceR.remainder)
  }
  implicit val helloResponseCodec = new Codec[HelloResponse] {
    val nonceCodec = int32L
    val userAgentCodec = ascii32
    override def encode(value: HelloResponse): Attempt[BitVector] =
      for {
        addressBits <- addressCodec.encode(value.remoteAddress)
        nonceBits <- nonceCodec.encode(value.nonce)
      } yield addressBits ++ nonceBits
    override def sizeBound: SizeBound = addressCodec.sizeBound + nonceCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[HelloResponse]] =
      for {
        addressR <- addressCodec.decode(bits)
        address = addressR.value
        nonceR <- nonceCodec.decode(addressR.remainder)
        nonce = nonceR.value
      } yield DecodeResult(HelloResponse(address, nonce), nonceR.remainder)
  }
  implicit val pexCodec = new Codec[Pex] {
    def identifierSetCodec = listOfN(int32L, identifierCodec)
    override def encode(value: Pex): Attempt[BitVector] =
      identifierSetCodec.encode(value.ids.toList)
    override def sizeBound: SizeBound = identifierSetCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[Pex]] =
      for {
        idsListR <- identifierSetCodec.decode(bits)
        idsList = idsListR.value
      } yield DecodeResult(Pex(idsList.toSet), idsListR.remainder)
  }
  implicit val pexResponseCodec = new Codec[PexResponse] {
    def identifierSetCodec = listOfN(int32L, identifierCodec)
    override def encode(value: PexResponse): Attempt[BitVector] =
      identifierSetCodec.encode(value.ids.toList)
    override def sizeBound: SizeBound = identifierSetCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[PexResponse]] =
      for {
        idsListR <- identifierSetCodec.decode(bits)
        idsList = idsListR.value
      } yield DecodeResult(PexResponse(idsList.toSet), idsListR.remainder)
  }
  implicit val shotCodec = new Codec[Shot] {
    val textCodec = variableSizeBytes(int32L, bytes)
    val expirationCodec = long(64)
    override def encode(value: Shot): Attempt[BitVector] =
      for {
        fromBytes <- identifierCodec.encode(value.from)
        toBytes <- identifierCodec.encode(value.to)
        protocolBytes <- int64L.encode(value.protocol)
        textBytes <- textCodec.encode(ByteVector(value.text))
        expirationBytes <- expirationCodec.encode(value.expiration)
      } yield fromBytes ++ toBytes ++ protocolBytes ++ textBytes ++ expirationBytes

    override def sizeBound: SizeBound =
      identifierCodec.sizeBound + identifierCodec.sizeBound + int64L.sizeBound + textCodec.sizeBound + expirationCodec.sizeBound

    override def decode(bits: BitVector): Attempt[DecodeResult[Shot]] =
      for {
        fromR <- identifierCodec.decode(bits)
        from = fromR.value
        toR <- identifierCodec.decode(fromR.remainder)
        to = toR.value
        protocolR <- int64L.decode(toR.remainder)
        protocol = protocolR.value
        textR <- textCodec.decode(toR.remainder)
        text = textR.value.toArray
        expirationR <- expirationCodec.decode(textR.remainder)
        expiration = expirationR.value
      } yield DecodeResult(Shot(from, to, protocol, text, expiration), expirationR.remainder)
  }

  implicit val multiShotCodec = new Codec[MultiShot] {
    val textCodec = variableSizeBytes(int32L, bytes)
    val expirationCodec = long(64)
    val toFieldCodec = listOfN(int32L, identifierCodec)

    override def encode(value: MultiShot): Attempt[BitVector] =
      for {
        fromBytes <- identifierCodec.encode(value.from)
        toBytes <- toFieldCodec.encode(value.to.toList)
        protocolBytes <- int64L.encode(value.protocol)
        textBytes <- textCodec.encode(ByteVector(value.text))
        expirationBytes <- expirationCodec.encode(value.expiration)
      } yield fromBytes ++ toBytes ++ protocolBytes ++ textBytes ++ expirationBytes

    override def sizeBound: SizeBound = identifierCodec.sizeBound + toFieldCodec.sizeBound + textCodec.sizeBound + expirationCodec.sizeBound

    override def decode(bits: BitVector): Attempt[DecodeResult[MultiShot]] =
      for {
        fromR <- identifierCodec.decode(bits)
        from = fromR.value
        toR <- toFieldCodec.decode(fromR.remainder)
        to = toR.value.toSet
        protocolR <- int64L.decode(toR.remainder)
        protocol = protocolR.value
        textR <- textCodec.decode(toR.remainder)
        text = textR.value.toArray
        expirationR <- expirationCodec.decode(textR.remainder)
        expiration = expirationR.value
      } yield DecodeResult(MultiShot(from, to, protocol, text, expiration), expirationR.remainder)
  }

  implicit val codec: Codec[Message] =
    discriminated[Message].by(byte)
      .typecase(1, implicitly[Codec[Hello]])
      .typecase(2, implicitly[Codec[HelloResponse]])
      .typecase(3, implicitly[Codec[Pex]])
      .typecase(4, implicitly[Codec[PexResponse]])
      .typecase(5, implicitly[Codec[Shot]])
      .typecase(6, implicitly[Codec[MultiShot]])

  def encode[A <: Message](message: A): Array[Byte] = codec.encode(message).toOption match {
    case Some(bitVector) => bitVector.toByteArray
    case None => throw new FailedEncodingError(s"Can not encode ${message.toString}")
  }

  def decode(bytes: Array[Byte]): Option[Message] = codec.decode(BitVector(bytes)).toOption.map { decodeResult =>
    decodeResult.value
  }

  case class FailedEncodingError(message: String) extends Error(message)

  sealed trait Message

  case class Hello(remoteAddress: Address, nonce: Int = new Random().nextInt) extends Message

  case class HelloResponse(remoteAddress: Address, nonce: Int) extends Message

  case class Pex(ids: Set[Identifier]) extends Message

  case class PexResponse(ids: Set[Identifier]) extends Message

  case class Shot(from: Identifier, to: Identifier, protocol: Long, text: Array[Byte], expiration: Long) extends Message

  case class MultiShot(from: Identifier, to: Set[Identifier], protocol: Long, text: Array[Byte], expiration: Long) extends Message
}
