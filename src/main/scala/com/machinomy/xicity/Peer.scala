package com.machinomy.xicity

import scala.concurrent.Future

class Peer(identifier: Identifier)

object Peer {
  def build(identifier: Identifier, seeds: Set[Connector]): Future[Peer] = {
    Future.failed(new Exception())
  }

  def buildServer(identifier: Identifier) = {

  }
}
