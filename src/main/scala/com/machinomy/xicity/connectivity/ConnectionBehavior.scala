package com.machinomy.xicity.connectivity

trait ConnectionBehavior {
  def onConnect(endpoint: Endpoint): ConnectionBehavior
  def onDisconnect(): ConnectionBehavior
  def onRead(bytes: Array[Byte]): ConnectionBehavior
  def onClose(): ConnectionBehavior
}
