package com.machinomy.stox.bootstrap

import argonaut._
import argonaut.Argonaut._
import akka.http.scaladsl.model.DateTime

case class Scan(lastScan: DateTime, nodes: Seq[Node])

object Scan {
  implicit val jsonDecoder: DecodeJson[Scan] = DecodeJson { c =>
    for {
      lastScan <- (c --\ "last_scan").as[Long].map(timestamp => DateTime(timestamp))
      nodes <- (c --\ "nodes").as[Seq[Node]]
    } yield Scan(lastScan, nodes)
  }
}
