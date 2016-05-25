package com.machinomy.xicity

case class RoutingTable(mapping: Map[Connector, Set[Identifier]]) {
  def +(m: (Connector, Set[Identifier])): RoutingTable = {
    val identifiers = mapping.getOrElse(m._1, Set.empty)
    RoutingTable(mapping.updated(m._1, identifiers ++ m._2))
  }

  def -(c: Connector): RoutingTable = RoutingTable(mapping - c)

  def -(m: (Connector, Set[Identifier])): RoutingTable = {
    val identifiers = mapping.getOrElse(m._1, Set.empty)
    RoutingTable(mapping.updated(m._1, identifiers -- m._2))
  }

  def identifiers: Set[Identifier] = mapping.values.fold(Set.empty) { case (a, b) => a ++ b }
}

object RoutingTable {
  def empty = RoutingTable(Map.empty)
}
