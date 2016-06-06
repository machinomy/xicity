package com.machinomy.xicity.connectivity

import java.net.{InetAddress, InetSocketAddress}

case class Endpoint(address: InetSocketAddress)

object Endpoint {
  val PORT = 4240

  def apply(host: String): Endpoint = {
    apply(host, PORT)
  }

  def apply(host: String, port: Int): Endpoint = {
    val address = new InetSocketAddress(host, port)
    Endpoint(address)
  }

  def apply(ipAddress: InetAddress, port: Int): Endpoint = {
    val address = new InetSocketAddress(ipAddress, port)
    Endpoint(address)
  }
}
