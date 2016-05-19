package com.machinomy.stox.node

object Transport {
  sealed trait Protocol {
    def value: Byte // Bit actually
  }
  case class UDP() extends Protocol {
    val value: Byte = 0
  }
  case class TCP() extends Protocol {
    val value: Byte = 1
  }
}
