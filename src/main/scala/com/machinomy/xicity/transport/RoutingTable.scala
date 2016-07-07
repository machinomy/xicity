package com.machinomy.xicity.transport

import com.machinomy.xicity.Identifier

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

  def closestEndpoints(id: Identifier, selfId: Identifier, n: Int = 1): Set[Endpoint] = {
    val a = reverseMapping.keys.toSeq.sortBy(i => Identifier.distance(i, id))
    val closestIds = a.take(n).toSet - selfId
    closestIds.flatMap(id => reverseMapping.getOrElse(id, Set.empty))
  }
}

object RoutingTable {
  def empty = RoutingTable(Map.empty)
}
