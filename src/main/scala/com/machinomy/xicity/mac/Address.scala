package com.machinomy.xicity.mac

import java.net.{InetAddress, InetSocketAddress}

case class Address(address: InetSocketAddress)

object Address {
  def apply(host: String): Address = apply(host, Parameters.DEFAULT_PORT)

  def apply(ipAddress: InetAddress): Address = Address(ipAddress, Parameters.DEFAULT_PORT)

  def apply(host: String, port: Int): Address = Address(new InetSocketAddress(host, port))

  def apply(ipAddress: InetAddress, port: Int): Address = Address(new InetSocketAddress(ipAddress, port))
}
