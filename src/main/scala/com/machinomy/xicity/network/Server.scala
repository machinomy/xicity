package com.machinomy.xicity.network

import java.net.{InetAddress, NetworkInterface}

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.mac
import com.machinomy.xicity.mac.Address

import scala.collection.JavaConversions._
import scala.util.{Failure, Success, Try}

class Server(node: Kernel.Wrap, parameters: mac.Parameters) extends Actor with ActorLogging {
  var serverActors: Set[ActorRef] = Set.empty

  override def preStart(): Unit = {
    Server.addresses match {
      case Success(addresses) =>
        log.info(s"Listening on ${addresses.size} interfaces")
        for (inetAddress <- addresses) {
          val address = Address(inetAddress, parameters.port)
          val serverBehaviorActor = context.actorOf(mac.ServerBehavior.props(node, parameters))
          val serverBehaviorWrap = mac.Server.BehaviorWrap(serverBehaviorActor)
          val serverActor = context.actorOf(mac.Server.props(address, serverBehaviorWrap))
          serverActors += serverActor
        }
      case Failure(exception) => throw exception
    }
  }

  override def receive: Receive = {
    case message: mac.Message.Single =>
      node.didReceive(message.from, message.to, message.text, message.expiration)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object Server extends NodeCompanion[Server] {
  def props(node: Kernel.Wrap) =
    Props(classOf[Server], node, mac.Parameters.default)

  def props(node: Kernel.Wrap, parameters: mac.Parameters) =
    Props(classOf[Server], node, parameters)

  def addresses: Try[Set[InetAddress]] = Try {
    val addresses = for {
      interface <- NetworkInterface.getNetworkInterfaces if interface.isUp
      address <- interface.getInetAddresses
    } yield address
    addresses.toSet
  }



  implicit val companion: NodeCompanion[Server] = this
}
