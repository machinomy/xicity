package com.machinomy.xicity

import com.machinomy.xicity.connectivity.Endpoint

import scala.concurrent.Future

class Peer(identifier: Identifier)

object Peer {
  def build(identifier: Identifier, seeds: Set[Endpoint]): Future[Peer] = {
    Future.failed(new Exception())
  }

  def buildServer(identifier: Identifier) = {

  }
}
