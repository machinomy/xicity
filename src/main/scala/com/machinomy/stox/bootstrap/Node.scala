package com.machinomy.stox.bootstrap

import java.net.InetAddress

import argonaut._
import argonaut.Argonaut._
import akka.http.scaladsl.model.DateTime
import com.machinomy.stox.sodium.PublicKey

import scala.util.Try

case class Node(ipv4: InetAddress,
                ipv6: Option[InetAddress],
                port: Int,
                tcpPorts: Seq[Int],
                publicKey: PublicKey,
                udpStatus: Boolean,
                tcpStatus: Boolean)

object Node {
  implicit val jsonDecoder: DecodeJson[Node] = DecodeJson { c =>
    for {
      ipv4 <- (c --\ "ipv4").as[String].map(address => InetAddress.getByName(address))
      ipv6 <- (c --\ "ipv6").as[Option[String]].map(stringOpt => stringOpt.flatMap(address => Try(InetAddress.getByName(address)).toOption))
      port <- (c --\ "port").as[Int]
      tcpPorts <- (c --\ "tcp_ports").as[Seq[Int]]
      publicKeyHex <- (c --\ "public_key").as[String]
      udpStatus <- (c --\ "status_udp").as[Boolean]
      tcpStatus <- (c --\ "status_tcp").as[Boolean]
      version <- (c --\ "version").as[String]
    } yield Node(ipv4, ipv6, port, tcpPorts, PublicKey(publicKeyHex), udpStatus, tcpStatus)
  }

  def apply(ipv4: String, port: Int, publicKey: String): Node = {
    Node(InetAddress.getByName(ipv4), None, port, Seq.empty, PublicKey(publicKey), true, false)
  }
}
