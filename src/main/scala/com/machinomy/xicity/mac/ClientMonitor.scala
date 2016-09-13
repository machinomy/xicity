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

import akka.actor.{Actor, ActorContext, ActorLogging, Props}
import com.machinomy.xicity.network.Kernel

import scala.util.Random

class ClientMonitor(kernel: Kernel.Wrap, parameters: Parameters) extends Actor with ActorLogging {
  val seeds = parameters.seeds
  val threshold = parameters.threshold

  assert(threshold >= 0)

  override def preStart(): Unit = {
    log.info(s"Started ClientMonitor")
    addClients(selectSeeds(threshold))
  }

  override def receive = {
    case something => throw new IllegalArgumentException(s"Not planned to receive anything, got: $something")
  }

  def selectSeeds(n: Byte): Set[Address] = Random.shuffle(seeds).take(n)

  def addClients(addresses: Set[Address]): Unit =
    for (address <- addresses) {
      log.info(s"Starting client for $addresses...")
      context.actorOf(Client.props(address, clientBehavior))
    }

  def clientBehavior()(implicit context: ActorContext) =
    Client.BehaviorWrap(context.actorOf(ClientBehavior.props(kernel, parameters)))
}

object ClientMonitor {
  def props(kernel: Kernel.Wrap, parameters: Parameters) = Props(classOf[ClientMonitor], kernel, parameters)
}
