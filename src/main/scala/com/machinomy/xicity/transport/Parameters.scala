package com.machinomy.xicity.transport

import scala.concurrent.duration._

trait Parameters {
  def port: Int
  def seeds: Set[Address]
  def threshold: Byte
  def serverAddress: Address
  def tickInterval: Duration
}

object Parameters {
  val default = new Parameters {
    override def port: Int = Address.PORT
    override def seeds: Set[Address] = Set(Address("localhost", port))
    override def threshold: Byte = 8
    override def serverAddress: Address = Address("0.0.0.0", port)
    override def tickInterval: Duration = 30.seconds
  }
}
