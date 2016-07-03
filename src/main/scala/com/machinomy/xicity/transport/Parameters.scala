package com.machinomy.xicity.transport

trait Parameters {
  def port: Int
  def seeds: Set[Address]
}
