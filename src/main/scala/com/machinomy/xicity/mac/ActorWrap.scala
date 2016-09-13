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

import akka.actor.{Actor, ActorRef}

trait ActorWrap {
  def actorRef: ActorRef
  def !(message: Any)(implicit sender: ActorRef = Actor.noSender): Unit = actorRef ! message
  def tell(msg: Any, sender: ActorRef): Unit = this.!(msg)(sender)
}
