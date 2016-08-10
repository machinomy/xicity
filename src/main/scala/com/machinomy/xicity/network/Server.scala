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
    Server.addresses(parameters) match {
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
    case message: mac.Message.Meaningful  =>
      node.passDownstream(message)
    case something => throw new IllegalArgumentException(s"Got unexpected $something")
  }
}

object Server extends NodeCompanion[Server] {
  def props(node: Kernel.Wrap) =
    Props(classOf[Server], node, mac.Parameters.default)

  def props(node: Kernel.Wrap, parameters: mac.Parameters) =
    Props(classOf[Server], node, parameters)

  def addresses(parameters: mac.Parameters): Try[Set[InetAddress]] = Try {
    parameters.bindAddress match {
      case Some(bindAddress) =>
        Set(bindAddress)
      case None =>
        for {
          interface <- NetworkInterface.getNetworkInterfaces.toSet
          if interface.isUp
          address <- interface.getInetAddresses
        } yield address
    }
  }



  implicit val companion: NodeCompanion[Server] = this
}
