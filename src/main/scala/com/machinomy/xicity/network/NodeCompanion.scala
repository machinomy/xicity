package com.machinomy.xicity.network

import akka.actor.Props
import com.machinomy.xicity.mac.Parameters

trait NodeCompanion[A] {
  def props(kernel: Kernel.Wrap, parameters: Parameters): Props
}
