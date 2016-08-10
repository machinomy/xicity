package com.machinomy.xicity.encoding

import scodec.Codec
import scodec.bits.BitVector

object Bytes {
  def encode[A: Codec](message: A): Array[Byte] =
    implicitly[Codec[A]].encode(message).toOption match {
      case Some(bitVector) => bitVector.toByteArray
      case None => throw FailedEncodingError(s"Can not encode ${message.toString}")
    }

  def decode[A: Codec](bytes: Array[Byte]): Option[A] =
    implicitly[Codec[A]].decode(BitVector(bytes)).toOption.map { decodeResult =>
      decodeResult.value
    }
}
