package com.machinomy.stox.dht

case class Packet[A](kind: Packet.Kind, payload: A)

object Packet {
  sealed abstract class Kind(byte: Byte)
  object PingRequest extends Kind(0x00)
  object PingResponse extends Kind(0x01)
  object NodesRequest extends Kind(0x02)
  object NodesResponse extends Kind(0x04)
  object CookieRequest extends Kind(0x18)
  object CookieResponse extends Kind(0x19)
  object CryptoHandshake extends Kind(0x1a)
  object CryptoData extends Kind(0x1b)
  object Crypto extends Kind(0x20)
  object LanDiscovery extends Kind(0x21)
  object OnionRequest0 extends Kind(0x80.toByte)
  object OnionRequest1 extends Kind(0x81.toByte)
  object OnionRequest2 extends Kind(0x82.toByte)
  object AnnounceRequest extends Kind(0x83.toByte)
  object AnnounceResponse extends Kind(0x84.toByte)
  object OnionDataRequest extends Kind(0x85.toByte)
  object OnionDataResponse extends Kind(0x86.toByte)
  object OnionResponse3 extends Kind(0x8c.toByte)
  object OnionResponse2 extends Kind(0x8d.toByte)
  object OnionResponse1 extends Kind(0x8e.toByte)
}
