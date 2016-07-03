package com.machinomy.xicity.transport

object DefaultParameters extends Parameters {
  override def port: Int = Address.PORT
  override def seeds: Set[Address] = Set(Address("localhost"))
  override def threshold: Byte = 8
}
