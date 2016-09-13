/*
 * Copyright 2016 Machinomy
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.machinomy.xicity.mac

import java.net.InetAddress

import akka.util.Timeout

import scala.concurrent.duration._

case class Parameters(port: Int,
                      seeds: Set[Address],
                      threshold: Byte,
                      tickInterval: FiniteDuration,
                      tickInitialDelay: FiniteDuration,
                      timeout: Timeout,
                      bindAddress: Option[InetAddress])

object Parameters {
  val DEFAULT_PORT = 4240

  val default = Parameters(
    port = DEFAULT_PORT,
    seeds = Set(Address("52.169.238.44")),
    threshold = 8,
    tickInterval = 3.seconds,
    tickInitialDelay = 1.seconds,
    timeout = Timeout(10.seconds),
    bindAddress = None
  )
}
