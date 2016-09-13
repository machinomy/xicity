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

import java.net.{InetAddress, InetSocketAddress}

case class Address(address: InetSocketAddress)

object Address {
  def apply(host: String): Address = apply(host, Parameters.DEFAULT_PORT)

  def apply(ipAddress: InetAddress): Address = Address(ipAddress, Parameters.DEFAULT_PORT)

  def apply(host: String, port: Int): Address = Address(new InetSocketAddress(host, port))

  def apply(ipAddress: InetAddress, port: Int): Address = Address(new InetSocketAddress(ipAddress, port))
}
