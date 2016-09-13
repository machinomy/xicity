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

package com.machinomy.identity

import com.machinomy.xicity.Identifier

import scala.concurrent.Future

// val chain = new identitiy.Chain()
// chain.ask(Identifier(345), Link("net.energy.belongs_to_grid"))
class Chain {
  val mockGridControllerIdentifier = Identifier(345)
  val mockDeviceIdentifiers = Set(Identifier(100), Identifier(110), Identifier(120), Identifier(130))

  def ask(authority: Identifier, link: Link): Future[Set[Relation]] = {
    val identifiers = if (authority == mockGridControllerIdentifier) mockDeviceIdentifiers else Set.empty
    val relations = for (identifier <- identifiers) yield Relation(link, identifier)
    Future.successful(relations)
  }
}
