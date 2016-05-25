package com.machinomy.xicity.connection

object InternetProtocol {
  sealed trait Family
  case object V4 extends Family
  case object V6 extends Family
}
