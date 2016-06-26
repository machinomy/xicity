package com.machinomy.xicity.connectivity

trait State {
  def onConnect(vertex: Vertex): State
  def onDisconnect(): State
  def onRead(bytes: Array[Byte]): State
  def onClose(): State
}
