package com.machinomy.xicity.protocol

import java.net.{InetAddress, InetSocketAddress}

import com.machinomy.xicity.protocol.JavaPayload.JavaCodec
import com.machinomy.xicity.{Connector, Identifier}
import scodec._
import scodec.bits._
import scodec.codecs._
import shapeless.Sized

import scala.util.Random

sealed trait Payload

object Payload {
  type Discriminator[A <: Payload] = scodec.codecs.Discriminator[Payload, A, Byte]

  case class VersionPayload(remoteConnector: Connector, nonce: Int, userAgent: String) extends Payload
  object VersionPayload{
    def apply(remoteConnector: Connector): VersionPayload = new VersionPayload(remoteConnector, new Random().nextInt(), "xicity/0.1")
  }
  case class PexPayload(ids: Set[Identifier]) extends Payload
  case class SingleMessagePayload(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Payload


  implicit val versionPayloadCodec = new Codec[VersionPayload] {
    val nonceCodec = int32L
    val userAgentCodec = ascii32
    override def encode(value: VersionPayload): Attempt[BitVector] =
      for {
        connectorBits <- connectorCodec.encode(value.remoteConnector)
        nonceBits <- nonceCodec.encode(value.nonce)
        userAgentBits <- userAgentCodec.encode(value.userAgent)
      } yield connectorBits ++ nonceBits ++ userAgentBits
    override def sizeBound: SizeBound = connectorCodec.sizeBound + nonceCodec.sizeBound + userAgentCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[VersionPayload]] =
      for {
        connectorR <- connectorCodec.decode(bits)
        connector = connectorR.value
        nonceR <- nonceCodec.decode(connectorR.remainder)
        nonce = nonceR.value
        userAgentR <- userAgentCodec.decode(nonceR.remainder)
        userAgent = userAgentR.value
      } yield DecodeResult(VersionPayload(connector, nonce, userAgent), userAgentR.remainder)
  }

  implicit val pexPayloadCodec = new Codec[PexPayload] {
    def identifierSetCodec = listOfN(int32L, identifierCodec)
    override def encode(value: PexPayload): Attempt[BitVector] =
      identifierSetCodec.encode(value.ids.toList)
    override def sizeBound: SizeBound = identifierSetCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[PexPayload]] =
      for {
        idsListR <- identifierSetCodec.decode(bits)
        idsList = idsListR.value
      } yield DecodeResult(PexPayload(idsList.toSet), idsListR.remainder)
  }

  implicit val singleMessagePayloadCodec = new Codec[SingleMessagePayload] {
    val textCodec = variableSizeBytes(int32L, bytes)
    val expirationCodec = long(64)
    override def encode(value: SingleMessagePayload): Attempt[BitVector] =
      for {
        fromBytes <- identifierCodec.encode(value.from)
        toBytes <- identifierCodec.encode(value.to)
        textBytes <- textCodec.encode(ByteVector(value.text))
        expirationBytes <- expirationCodec.encode(value.expiration)
      } yield fromBytes ++ toBytes ++ textBytes ++ expirationBytes

    override def sizeBound: SizeBound =
      identifierCodec.sizeBound + identifierCodec.sizeBound + textCodec.sizeBound + expirationCodec.sizeBound

    override def decode(bits: BitVector): Attempt[DecodeResult[SingleMessagePayload]] =
      for {
        fromR <- identifierCodec.decode(bits)
        from = fromR.value
        toR <- identifierCodec.decode(fromR.remainder)
        to = toR.value
        textR <- textCodec.decode(toR.remainder)
        text = textR.value.toArray
        expirationR <- expirationCodec.decode(textR.remainder)
        expiration = expirationR.value
      } yield DecodeResult(SingleMessagePayload(from, to, text, expiration), expirationR.remainder)
  }

  val inetAddressCodec = new Codec[InetAddress] {
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

  val connectorCodec: Codec[Connector] = new Codec[Connector] {
    val portCodec = uint16L
    override def encode(value: Connector): Attempt[BitVector] =
      for {
        address <- inetAddressCodec.encode(value.address.getAddress)
        port <- portCodec.encode(value.address.getPort)
      } yield address ++ port
    override def sizeBound: SizeBound = inetAddressCodec.sizeBound + portCodec.sizeBound
    override def decode(bits: BitVector): Attempt[DecodeResult[Connector]] =
      for {
        inetAddressR <- inetAddressCodec.decode(bits)
        portR <- portCodec.decode(inetAddressR.remainder)
      } yield DecodeResult(Connector(inetAddressR.value, portR.value), portR.remainder)
  }

  val identifierCodec: Codec[Identifier] = new Codec[Identifier] {
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

  implicit val codec: Codec[Payload] =
    discriminated[Payload].by(byte)
      .typecase(1, implicitly[Codec[VersionPayload]])
      .typecase(2, implicitly[Codec[PexPayload]])
      .typecase(3, implicitly[Codec[SingleMessagePayload]])
}
