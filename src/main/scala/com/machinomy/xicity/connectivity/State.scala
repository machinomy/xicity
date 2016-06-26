package com.machinomy.xicity.connectivity

trait State {
  def onConnect(endpoint: Endpoint): State
  def onDisconnect(): State
  def onRead(bytes: Array[Byte]): State
  def onClose(): State
}
