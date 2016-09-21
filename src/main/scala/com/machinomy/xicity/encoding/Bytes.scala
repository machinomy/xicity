/*
 * Copyright 2016 Machinomy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.machinomy.xicity.encoding

import akka.util.ByteString
import scodec.bits.BitVector
import scodec.{Codec, DecodeResult}

object Bytes {
  def encode[A: Codec](message: A): Array[Byte] =
    implicitly[Codec[A]].encode(message).toOption match {
      case Some(bitVector) => bitVector.toByteArray
      case None => throw FailedEncodingError(s"Can not encode ${message.toString}")
    }

  def decode[A: Codec](byteString: ByteString): Option[DecodeResult[A]] =
    decode(byteString.toArray)

  def decode[A: Codec](bytes: Array[Byte]): Option[DecodeResult[A]] =
    implicitly[Codec[A]].decode(BitVector(bytes)).toOption
}
