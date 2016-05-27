package com.machinomy.xicity

import akka.actor.FSM.{Failure, Normal}
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, FSM, Props}
import akka.io.{IO, Tcp}

import scala.concurrent.duration._
import akka.util.ByteString
import com.machinomy.xicity.protocol._
import akka.pattern.ask

import scala.util.Random

class PeerConnection(node: ActorRef) extends FSM[PeerConnection.State, PeerConnection.Data] with ActorLogging {

  implicit val timeout: akka.util.Timeout = akka.util.Timeout(30.seconds)
  implicit val executionContext = context.system.dispatcher

  startWith(PeerConnection.Initial, PeerConnection.NoData)

  when(PeerConnection.Initial) {
    case Event(PeerConnection.IncomingConnection(tcp, remote, local), _) =>
      log.info(s"Got incoming connection from $remote")
      goto(PeerConnection.Connected).using(PeerConnection.ConnectionData(tcp, remote, local))
    case Event(PeerConnection.OutgoingConnection(tcp, remote, local), _) =>
      log.info(s"Got outgoing connection to $remote")
      val versionPayload = VersionPayload(remote)
      tcp ! Tcp.Write(ByteString(WiredPayload.toBytes(versionPayload)))
      val connectionData = PeerConnection.ConnectionData(tcp, remote, local)
      val nextState = PeerConnection.WaitingForVersionPayloadData(versionPayload.nonce, connectionData)
      goto(PeerConnection.WaitingForVersionPayloadReply) using nextState
  }

  when(PeerConnection.WaitingForVersionPayloadReply) {
    case Event(Tcp.Received(byteString), state: PeerConnection.WaitingForVersionPayloadData) =>
      parse(byteString) {
        case v: VersionPayload if v.nonce == state.nonce =>
          log.info(s"Received correct VersionPayload reply")
          sendPex(state.connectionData)
          goto(PeerConnection.WaitingForPexPayloadReply) using PeerConnection.WaitingForPexPayloadData(state.connectionData)
        case v: VersionPayload => stop(Failure(s"Expected VersionPayload nonce to be ${state.nonce}, got ${v.nonce}"))
      }
  }

  when(PeerConnection.WaitingForPexPayloadReply) {
    case Event(Tcp.Received(byteString), state: PeerConnection.WaitingForPexPayloadData) =>
      parse(byteString) {
        case v: PexPayload =>
          log.info(s"Received Pex payload: $v")
          node ! PeerNode.AddRoutingTableCommand(state.connectionData.remoteConnector, v.ids)
          goto(PeerConnection.Connected) using state.connectionData
      }
  }

  when(PeerConnection.Connected, stateTimeout = 10.seconds) {
    case Event(Tcp.Received(byteString), state: PeerConnection.ConnectionData) =>
      parse(byteString) {
        case VersionPayload(remoteConnector, nonce, userAgent) =>
          val versionPayloadReply = VersionPayload(state.remoteConnector).copy(nonce=nonce)
          log.info(s"Replied using $versionPayloadReply to $remoteConnector")
          state.wire ! Tcp.Write(ByteString(WiredPayload.toBytes(versionPayloadReply)))
          stay
        case PexPayload(ids) =>
          log.info(s"Got $ids from ${state.remoteConnector}")
          node ! PeerNode.AddRoutingTableCommand(state.remoteConnector, ids)
          sendPex(state)
          stay
        case SingleMessagePayload(from, to, text, expiration) =>
          log.info(s"GOT SINGLE MESSAGE PAYLOAD: $from, $to, $text")
          node ! PeerNode.ReceivedSingleMessage(from, to, text, expiration)
          stay
      }
    case Event(PeerConnection.SingleMessage(from, to, text, expiration), state: PeerConnection.ConnectionData) =>
      state.wire ! Tcp.Write(ByteString(WiredPayload.toBytes(SingleMessagePayload(from, to, text, expiration))))
      stay
    case Event(StateTimeout, state: PeerConnection.ConnectionData) =>
      sendPex(state)
      goto(PeerConnection.WaitingForPexPayloadReply) using PeerConnection.WaitingForPexPayloadData(state)
  }

  whenUnhandled {
    case Event(Tcp.PeerClosed, _) =>
      log.info(s"Stopping peer connection: received Tcp.PeerClosed")
      stop(Normal)
    case Event(e, s) =>
      log.warning(s"Received $e while having $s in $stateName")
      stay
  }

  initialize()

  def parse(bytes: Array[Byte])(f: PartialFunction[Payload, State]): State = {
    WiredPayload.fromBytes(bytes) match {
      case Some(payload) =>
        if (f.isDefinedAt(payload)) {
          f(payload)
        } else {
          log.warning(s"Can not handle $payload")
          stay
        }
      case None => {
        val string = bytes.map(_.toChar).mkString
        stop(Failure(s"Expected payload, got $string"))
      }
    }
  }

  def parse(byteString: ByteString)(f: PartialFunction[Payload, State]): State = parse(byteString.toArray)(f)

  def sendPex(connectionData: PeerConnection.ConnectionData) = {
    log.info(s"Going to ask for Pex")
    for {
      identifiers <- node.ask(PeerNode.GetKnownIdentifiersCommand(connectionData.remoteConnector)).mapTo[Set[Identifier]]
      identifier <- node.ask(PeerNode.GetIdentifierCommand).mapTo[Identifier]
    } {
      connectionData.wire ! Tcp.Write(ByteString(WiredPayload.toBytes(PexPayload(identifiers + identifier))))
    }
  }
}

object PeerConnection {
  sealed trait Protocol
  case class OutgoingConnection(tcp: ActorRef, remote: Connector, local: Connector) extends Protocol
  case class IncomingConnection(tcp: ActorRef, remote: Connector, local: Connector) extends Protocol
  case class SingleMessage(from: Identifier, to: Identifier, text: Array[Byte], expiration: Long) extends Protocol

  sealed trait State
  case object Initial extends State
  case object Connected extends State
  case object WaitingForVersionPayloadReply extends State
  case object WaitingForPexPayloadReply extends State

  sealed trait Data
  case object NoData extends Data
  case class ConnectionData(wire: ActorRef, remoteConnector: Connector, localConnector: Connector) extends Data
  case class WaitingForVersionPayloadData(nonce: Long, connectionData: ConnectionData) extends Data
  case class WaitingForPexPayloadData(connectionData: ConnectionData) extends Data

  def props(node: ActorRef): Props = Props(classOf[PeerConnection], node)
}
