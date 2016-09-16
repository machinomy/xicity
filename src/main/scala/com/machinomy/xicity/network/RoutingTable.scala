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

package com.machinomy.xicity.network

import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.Endpoint

case class RoutingTable(mapping: Map[Endpoint, Set[Identifier]]) {
  def +(m: (Endpoint, Set[Identifier])): RoutingTable = {
    val identifiers = mapping.getOrElse(m._1, Set.empty)
    RoutingTable(mapping.updated(m._1, identifiers ++ m._2))
  }

  def -(c: Endpoint): RoutingTable = RoutingTable(mapping - c)

  def -(m: (Endpoint, Set[Identifier])): RoutingTable = {
    val identifiers = mapping.getOrElse(m._1, Set.empty)
    RoutingTable(mapping.updated(m._1, identifiers -- m._2))
  }

  def identifiers: Set[Identifier] = mapping.values.fold(Set.empty)(_ ++ _)
  def identifiers(except: Endpoint): Set[Identifier] = (mapping - except).values.fold(Set.empty)(_ ++ _)

  lazy val reverseMapping: Map[Identifier, Set[Endpoint]] = {
    val explosion: Map[Identifier, Endpoint] = for {
      (c, ids) <- mapping
      id <- ids
    } yield (id, c)
    explosion.foldLeft(Map.empty[Identifier, Set[Endpoint]]) { case (acc, (id, c)) =>
      acc.updated(id, acc.getOrElse(id, Set.empty) + c)
    }
  }

  def closestEndpoints(id: Identifier, exclude: Identifier, n: Int = 1): Set[Endpoint] = {
    closestEndpoints(id, Set(exclude), n)
  }

  def closestEndpoints(id: Identifier, exclude: Set[Identifier], n: Int = 1): Set[Endpoint] = {
    val a = reverseMapping.keys.toSeq.sortBy(i => Identifier.distance(i, id))
    val closestIds = a.take(n).toSet -- exclude
    closestIds.flatMap(id => reverseMapping.getOrElse(id, Set.empty))
  }
}

object RoutingTable {
  def empty = RoutingTable(Map.empty)
}
