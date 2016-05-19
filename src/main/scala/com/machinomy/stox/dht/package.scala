package com.machinomy.stox

import com.machinomy.stox.sodium.CipherText
import scodec._
import scodec.codecs._
import scodec.bits._

package object dht {
  implicit val cipherTextCodec: Codec[CipherText] = bytes.xmap(
    (bv: ByteVector) => CipherText(bv.toArray),
    (ct: CipherText) => ByteVector(ct.bytes)
  )
}
