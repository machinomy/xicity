package com.machinomy.xicity

import akka.actor.FSM.{Failure, Normal}
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, FSM, Props}
import akka.io.{IO, Tcp}

import scala.concurrent.duration._
import akka.util.ByteString
import com.machinomy.xicity.protocol._
import akka.pattern.ask
import com.machinomy.xicity.connectivity.Connector
import com.machinomy.xicity.protocol.Payload.Discriminator
import scodec.{Attempt, DecodeResult}

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
      val versionPayload = Payload.VersionPayload(remote)
      write(tcp, versionPayload)
      val connectionData = PeerConnection.ConnectionData(tcp, remote, local)
      val nextState = PeerConnection.WaitingForVersionPayloadData(versionPayload.nonce, connectionData)
      goto(PeerConnection.WaitingForVersionPayloadReply) using nextState
  }

  when(PeerConnection.WaitingForVersionPayloadReply) {
    case Event(Tcp.Received(byteString), state: PeerConnection.WaitingForVersionPayloadData) =>
      parse(byteString) {
        case v: Payload.VersionPayload if v.nonce == state.nonce =>
          log.info(s"Received correct VersionPayload reply")
          sendPex(state.connectionData)
          goto(PeerConnection.WaitingForPexPayloadReply) using PeerConnection.WaitingForPexPayloadData(state.connectionData)
        case v: Payload.VersionPayload => stop(Failure(s"Expected VersionPayload nonce to be ${state.nonce}, got ${v.nonce}"))
      }
  }

  when(PeerConnection.WaitingForPexPayloadReply) {
    case Event(Tcp.Received(byteString), state: PeerConnection.WaitingForPexPayloadData) =>
      parse(byteString) {
        case v: Payload.PexPayload =>
          //log.info(s"Received Pex payload: $v")
          node ! PeerNode.AddRoutingTableCommand(state.connectionData.remoteConnector, v.ids)
          goto(PeerConnection.Connected) using state.connectionData
      }
  }

  when(PeerConnection.Connected, stateTimeout = 10.seconds) {
    case Event(Tcp.Received(byteString), state: PeerConnection.ConnectionData) =>
      parse(byteString) {
        case Payload.VersionPayload(remoteConnector, nonce, userAgent) =>
          val versionPayloadReply = Payload.VersionPayload(state.remoteConnector).copy(nonce=nonce)
          log.info(s"Replied using $versionPayloadReply to $remoteConnector")
          write(state.wire, versionPayloadReply)
          stay
        case Payload.PexPayload(ids) =>
          log.info(s"Got $ids from ${state.remoteConnector}")
          node ! PeerNode.AddRoutingTableCommand(state.remoteConnector, ids)
          stay
        case Payload.SingleMessagePayload(from, to, text, expiration) =>
          log.info(s"GOT SINGLE MESSAGE PAYLOAD: $from, $to, $text")
          node ! PeerNode.ReceivedSingleMessage(from, to, text, expiration)
          stay
      }
    case Event(PeerConnection.SingleMessage(from, to, text, expiration), state: PeerConnection.ConnectionData) =>
      write(state.wire, Payload.SingleMessagePayload(from, to, text, expiration))
      stay
    case Event(PeerNode.SendSingleMessageCommand(from, to, text, expiration), state: PeerConnection.ConnectionData) =>
      write(state.wire, Payload.SingleMessagePayload(from, to, text, expiration))
      stay
    case Event(StateTimeout, state: PeerConnection.ConnectionData) =>
      sendPex(state)
      stay
    case Event(Tcp.PeerClosed, state: PeerConnection.ConnectionData) =>
      node ! PeerNode.RemoveRoutingTableCommand(state.remoteConnector)
      node ! PeerNode.RemoveRunningClient(state.remoteConnector)
      stop(FSM.Shutdown)
    case Event(e, f) =>
      log.info(s"DEBUG: ${e.toString}, ${f.toString}")
      stay
  }

  whenUnhandled {
    case Event(Tcp.PeerClosed, state: PeerConnection.ConnectionData) =>
      stopOnPeerClose(state.remoteConnector)
    case Event(Tcp.PeerClosed, state: PeerConnection.WaitingForPexPayloadData) =>
      stopOnPeerClose(state.connectionData.remoteConnector)
    case Event(Tcp.PeerClosed, state: PeerConnection.WaitingForVersionPayloadData) =>
      stopOnPeerClose(state.connectionData.remoteConnector)
    case Event(e, s) =>
      log.warning(s"Received $e while having $s in $stateName")
      stay
  }

  initialize()

  def stopOnPeerClose(connector: Connector): State = {
    log.info(s"Stopping peer connection: received Tcp.PeerClosed")
    node ! PeerNode.RemoveRoutingTableCommand(connector)
    node ! PeerNode.RemoveRunningClient(connector)
    stop(FSM.Shutdown)
  }

  def write[A <: Payload](tcp: ActorRef, payload: A) = {
    tcp ! Tcp.Write(ByteString(WiredPayload.toBytes(payload)))
  }

  def parse(bytes: Array[Byte])(f: PartialFunction[Payload, State]): State = {
    WiredPayload.decode(bytes) match {
      case Attempt.Successful(DecodeResult(payload, remainder)) =>
        if (remainder.nonEmpty) {
          self ! Tcp.Received(ByteString(remainder.toByteArray))
        }
        if (f.isDefinedAt(payload)) {
          f(payload)
        } else {
          log.warning(s"Can not handle $payload")
          stay
        }
      case Attempt.Failure(cause) =>
        log.error(cause.toString, "Expected payload")
        stop(Failure(s"Expected payload, got ${cause.toString}"))
    }
  }

  def parse(byteString: ByteString)(f: PartialFunction[Payload, State]): State = parse(byteString.toArray)(f)

  def sendPex(connectionData: PeerConnection.ConnectionData) = {
    //log.info(s"Going to ask for Pex")
    for {
      identifiers <- node.ask(PeerNode.GetKnownIdentifiersCommand(connectionData.remoteConnector)).mapTo[Set[Identifier]]
      identifier <- node.ask(PeerNode.GetIdentifierCommand).mapTo[Identifier]
    } {
      write(connectionData.wire, Payload.PexPayload(identifiers + identifier))
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
