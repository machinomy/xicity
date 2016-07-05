package com.machinomy.xicity.transport

import akka.util.Timeout

import scala.concurrent.duration._

trait Parameters {
  def port: Int
  def seeds: Set[Address]
  def threshold: Byte
  def serverAddress: Address
  def tickInterval: FiniteDuration
  def tickInitialDelay: FiniteDuration
  def timeout: Timeout
}

object Parameters {
  val default = new Parameters {
    override def port: Int = Address.PORT
    override def seeds: Set[Address] = Set(Address("localhost", port))
    override def threshold: Byte = 8
    override def serverAddress: Address = Address("0.0.0.0", port)
    override def tickInterval: FiniteDuration = 30.seconds
    override def tickInitialDelay: FiniteDuration = 2.seconds
    override def timeout: Timeout = Timeout(10.seconds)
  }
}
