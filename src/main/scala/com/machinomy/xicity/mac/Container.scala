package com.machinomy.xicity.mac

object Container {
  sealed trait Container
  case class Downstream(payload: Message.Meaningful)
}
