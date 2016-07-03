package com.machinomy.xicity

import com.machinomy.xicity.transport.Address

import scala.concurrent.Future

class Peer(identifier: Identifier)

object Peer {
  def build(identifier: Identifier, seeds: Set[Address]): Future[Peer] = {
    Future.failed(new Exception())
  }

  def buildServer(identifier: Identifier) = {

  }
}
