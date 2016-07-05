package com.machinomy.xicity.transport

trait Parameters {
  def port: Int
  def seeds: Set[Address]
  def threshold: Byte
  def serverAddress: Address
}

object Parameters {
  val default = new Parameters {
    override def port: Int = Address.PORT
    override def seeds: Set[Address] = Set(Address("localhost", port))
    override def threshold: Byte = 8
    override def serverAddress: Address = Address("0.0.0.0", port)
  }
}
