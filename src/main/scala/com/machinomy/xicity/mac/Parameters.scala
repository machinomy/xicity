package com.machinomy.xicity.mac

import akka.util.Timeout

import scala.concurrent.duration._

case class Parameters(port: Int,
                      seeds: Set[Address],
                      threshold: Byte,
                      serverAddress: Address,
                      tickInterval: FiniteDuration,
                      tickInitialDelay: FiniteDuration,
                      timeout: Timeout)

object Parameters {
  val DEFAULT_PORT = 4240

  val default = Parameters(
    port = DEFAULT_PORT,
    seeds = Set(),
    threshold = 8,
    serverAddress = Address("0.0.0.0", DEFAULT_PORT),
    tickInterval = 3.seconds,
    tickInitialDelay = 1.seconds,
    timeout = Timeout(10.seconds)
  )
}
