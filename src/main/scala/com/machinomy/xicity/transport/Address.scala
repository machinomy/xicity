package com.machinomy.xicity.transport

import java.net.{InetAddress, InetSocketAddress}

case class Address(address: InetSocketAddress)

object Address {
  val PORT = 4240

  def apply(host: String): Address = apply(host, PORT)

  def apply(host: String, port: Int): Address = Address(new InetSocketAddress(host, port))

  def apply(ipAddress: InetAddress, port: Int): Address = Address(new InetSocketAddress(ipAddress, port))
}
