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

package com.machinomy.xicity.examples

import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.{Address, Parameters}
import com.machinomy.xicity.network.{FullNode, Peer, PeerBase}
import com.typesafe.scalalogging.LazyLogging

import scala.util.Try

object SeedApp extends App with LazyLogging {
  case class Config(exclude: Seq[String] = Seq.empty, bind: Option[String] = None)

  val parser = new scopt.OptionParser[Config]("xicity") {
    head("xicity")

    opt[Seq[String]]('x', "exclude").valueName("<ip>,<ip>,...").action( (x, c) =>
      c.copy(exclude = x)
    ).text("addresses not to connect to")

    opt[String]('b', "bind").action( (x, c) =>
      c.copy(bind = Some(x))
    ).text("address to bind to")
  }

  parser.parse(args, Config()) match {
    case Some(config) =>
      val exclusions: Seq[Address] =
        for {
          string <- config.exclude
          maybeAddress = Try(InetAddress.getByName(string))
          if maybeAddress.isSuccess
        } yield Address(maybeAddress.get)
      val bind: Option[InetAddress] =
        for {
          string <- config.bind
          maybeAddress = Try(InetAddress.getByName(string))
          if maybeAddress.isSuccess
        } yield maybeAddress.get
      run(effectiveParameters(exclusions, bind))
    case None =>
  }


  class Node(identifier: Identifier, parameters: Parameters) extends Actor with ActorLogging {
    override def preStart(): Unit = {
      logger.info(s"Using $parameters")
      context.actorOf(PeerBase.props[FullNode](identifier, self, parameters), s"peer-${identifier.number}")
    }

    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"I am Ready")
    }
  }

  def effectiveParameters(exclusions: Seq[Address], bind: Option[InetAddress]): Parameters = {
    val seeds = Parameters.default.seeds -- exclusions
    Parameters.default.copy(seeds = seeds, bindAddress = bind)
  }

  def run(parameters: Parameters): Unit = {
    val system = ActorSystem("xicity")
    val identifier = Identifier.random
    system.actorOf(Props(classOf[Node], identifier, parameters), s"peer-${identifier.number}")
  }
}
