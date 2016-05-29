package com.machinomy.xicity.protocol

import com.machinomy.xicity.Identifier

case class SingleMessagePayload(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends JavaPayload(SingleMessagePayload.name)
object SingleMessagePayload extends PayloadCompanion {
  def name = "single"
}
