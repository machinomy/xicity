package com.machinomy.xicity.protocol

import com.machinomy.xicity.Connector
import com.machinomy.xicity.protocol.PayloadCompanion

import scala.util.Random

case class VersionPayload(remoteConnector: Connector,
                          nonce: Long,
                          userAgent: String) extends Payload(VersionPayload.name)

object VersionPayload extends PayloadCompanion {
  val name = "version"
  def apply(remoteConnector: Connector): VersionPayload = new VersionPayload(remoteConnector, new Random().nextLong(), "xicity/0.1")
}
