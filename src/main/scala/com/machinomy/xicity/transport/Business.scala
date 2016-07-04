package com.machinomy.xicity.transport

import com.machinomy.xicity.transport.Message.Shot

trait Business {
  def didConnect(): Unit = {}
  def didRead(message: Shot): Unit = {}
}
