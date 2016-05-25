package com.machinomy.xicity

import java.net.{InetAddress, InetSocketAddress}

case class Connector(address: InetSocketAddress)

object Connector {
  val PORT = 4240

  def apply(host: String): Connector = {
    apply(host, PORT)
  }

  def apply(host: String, port: Int): Connector = {
    val address = new InetSocketAddress(host, port)
    Connector(address)
  }

  def apply(ipAddress: InetAddress, port: Int): Connector = {
    val address = new InetSocketAddress(ipAddress, port)
    Connector(address)
  }
}
