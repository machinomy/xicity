package com.machinomy.xicity.connectivity

trait ConnectionBehavior {
  def didConnect(endpoint: Endpoint): ConnectionBehavior
  def didDisconnect(): ConnectionBehavior
  def didRead(bytes: Array[Byte]): ConnectionBehavior
  def didClose(): ConnectionBehavior
}
