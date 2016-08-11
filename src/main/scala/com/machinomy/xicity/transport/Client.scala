package com.machinomy.xicity.transport

import akka.actor._
import akka.pattern.ask
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.{Message, Parameters}
import com.machinomy.xicity.network.{Client, Peer, PeerBase}

import scala.concurrent.Future
import scala.language.postfixOps
import scala.util.Random

object Client {
  case class State(actorRef: Option[ActorRef])

  sealed trait Comm
  case object Start extends Comm
  case object DidStart extends Comm
  case object Stop extends Comm
  case class Request(receiver: Identifier, text: String) extends Comm
  case class Response(text: String) extends Comm

  class ClientActor(identifier: Identifier, parameters: Parameters) extends Actor with ActorLogging {
    private var upstream: ActorRef = _
    private var track: Map[Int, ActorRef] = Map.empty[Int, ActorRef]
    private var callback: ActorRef = _

    override def receive: Receive = {
      case Start =>
        callback = sender()
        val upstreamProps = PeerBase.props[Client](identifier, self, parameters)
        upstream = context.actorOf(upstreamProps, "peer")
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
        callback ! DidStart
      case Stop =>
        context.stop(self)
      case Request(receiver, text) =>
        val id = Random.nextInt
        track += (id -> sender())
        upstream ! Message.Request(identifier, receiver, text.getBytes, DateTime.now() + 1.second, id)
      case Peer.Received(payload: Message.Meaningful) =>
        payload match {
          case message: Message.Single =>
            log info s"Received single message: $message"
          case message: Message.Response =>
            log info s"Received response: $message"
            track.get(message.id) match {
              case Some(actorRef) =>
                track -= message.id
                val text = message.text.toList.mkString
                actorRef ! Response(text)
              case None => // Do Nothing
            }
          case something =>
            throw new IllegalArgumentException(s"Got unexpected $something")
        }
      case something =>
        upstream ! something
    }
  }

  def start(parameters: Parameters)(implicit actorRefFactory: ActorRefFactory): Future[State] = {
    val identifier = Identifier.random
    val actorRef = actorRefFactory.actorOf(Props(classOf[ClientActor], identifier, parameters))
    for {
      _ <- actorRef ? Start
    } yield State(Some(actorRef))
  }

  def stop(state: State): Future[State] = {
    for {
      actorRef <- state.actorRef
    } {
      actorRef ! Stop
    }
    Future.successful(State(None))
  }

  def ask(state: State, receiver: Identifier, request: String): Future[String] = state.actorRef match {
    case Some(actorRef) =>
      for {
        response <- actorRef.ask(Request(receiver, request)).mapTo[Response]
      } yield response.text
    case None =>
      Future.failed(new IllegalArgumentException("State is not running"))
  }
}
