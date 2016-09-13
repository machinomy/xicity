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

import akka.actor.{Actor, ActorLogging}

import scala.reflect.ClassTag

abstract class EventHandler[A](implicit classTag: ClassTag[A]) extends Actor with ActorLogging {
  type Handle = PartialFunction[A, Unit]

  override def receive: Receive = {
    case something => something match {
      case event: A => handle(event)
      case anything => throw new IllegalArgumentException(s"Received unexpected $anything")
    }
  }

  def handle: Handle
}
