package com.machinomy.xicity

import com.machinomy.xicity.transport.Address

case class RoutingTable(mapping: Map[Address, Set[Identifier]]) {
  def +(m: (Address, Set[Identifier])): RoutingTable = {
    val identifiers = mapping.getOrElse(m._1, Set.empty)
    RoutingTable(mapping.updated(m._1, identifiers ++ m._2))
  }

  def -(c: Address): RoutingTable = RoutingTable(mapping - c)

  def -(m: (Address, Set[Identifier])): RoutingTable = {
    val identifiers = mapping.getOrElse(m._1, Set.empty)
    RoutingTable(mapping.updated(m._1, identifiers -- m._2))
  }

  def identifiers: Set[Identifier] = mapping.values.fold(Set.empty) { case (a, b) => a ++ b }

  lazy val reverseMapping: Map[Identifier, Set[Address]] = {
    val explosion: Map[Identifier, Address] = for {
      (c, ids) <- mapping
      id <- ids
    } yield (id, c)
    explosion.foldLeft(Map.empty[Identifier, Set[Address]]) { case (acc, (id, c)) =>
        acc.updated(id, acc.getOrElse(id, Set.empty) + c)
    }
  }

  def closestAddresses(id: Identifier, selfId: Identifier, n: Int = 1): Set[Address] = {
    val a = reverseMapping.keys.toSeq.sortBy(i => Identifier.distance(i, id))
    println(id)
    println(a)
    val closestIds = a.take(n).toSet - selfId
    closestIds.flatMap(id => reverseMapping.getOrElse(id, Set.empty))
  }
}

object RoutingTable {
  def empty = RoutingTable(Map.empty)
}
