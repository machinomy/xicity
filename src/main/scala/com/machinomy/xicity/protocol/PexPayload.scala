package com.machinomy.xicity.protocol

import com.machinomy.xicity.Identifier

case class PexPayload(ids: Set[Identifier]) extends Payload(PexPayload.name)
object PexPayload extends PayloadCompanion {
  def name = "pex"
}
