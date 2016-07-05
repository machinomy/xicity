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
