package com.machinomy.xicity.transport

trait Parameters {
  def port: Int
  def seeds: Set[Address]
  def threshold: Byte
}

object Parameters {
  val default = new Parameters {
    override def port: Int = Address.PORT
    override def seeds: Set[Address] = Set(Address("localhost"))
    override def threshold: Byte = 8
  }
}
