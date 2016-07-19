package com.machinomy.xicity.mac

import akka.util.Timeout

import scala.concurrent.duration._

case class Parameters(port: Int,
                      seeds: Set[Address],
                      threshold: Byte,
                      tickInterval: FiniteDuration,
                      tickInitialDelay: FiniteDuration,
                      timeout: Timeout)

object Parameters {
  val DEFAULT_PORT = 4240

  val default = Parameters(
    port = DEFAULT_PORT,
    seeds = Set(Address("52.169.238.44")),
    threshold = 8,
    tickInterval = 3.seconds,
    tickInitialDelay = 1.seconds,
    timeout = Timeout(10.seconds)
  )
}
