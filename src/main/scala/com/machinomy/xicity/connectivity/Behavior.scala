package com.machinomy.xicity.connectivity

trait Behavior {
  def onConnect(vertex: Vertex): Behavior
  def onDisconnect(): Behavior
  def onRead(bytes: Array[Byte]): Behavior
}
