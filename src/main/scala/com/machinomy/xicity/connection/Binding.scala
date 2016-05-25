package com.machinomy.xicity.connection

import java.net.InetAddress

case class Binding(any: Boolean = false,
                   addresses: Seq[InetAddress] = Seq.empty,
                   interfaces: Seq[String] = Seq.empty,
                   protocols: Seq[InternetProtocol.Family] = Seq.empty) {
  def +(address: InetAddress): Binding = Binding(any, addresses :+ address, interfaces, protocols)
  def +(interface: String): Binding = Binding(any, addresses, interfaces :+ interface, protocols)
  def +(protocol: InternetProtocol.Family) = Binding(any, addresses, interfaces, protocols :+ protocol)

  def anyInterface = interfaces.isEmpty
  def anyProtocol = protocols.isEmpty
  def isV4 = anyProtocol || protocols.contains(InternetProtocol.V4)
  def isV6 = anyProtocol || protocols.contains(InternetProtocol.V6)
}


