package com.machinomy.xicity

import akka.actor.FSM.{Failure, Normal}
import akka.actor.{Actor, ActorLogging, ActorRef, Cancellable, FSM, Props}
import akka.io.{IO, Tcp}

import scala.concurrent.duration._
import akka.util.ByteString
import com.machinomy.xicity.protocol._

class PeerConnection extends FSM[PeerConnection.State, PeerConnection.Data] with ActorLogging {

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
          goto(PeerConnection.Connected) using state.connectionData
        case v: VersionPayload => stop(Failure(s"Expected VersionPayload nonce to be ${state.nonce}"))
      }
  }

  when(PeerConnection.Connected) {
    case Event(Tcp.Received(byteString), state: PeerConnection.ConnectionData) =>
      parse(byteString) {
        case v @ VersionPayload(remoteConnector, nonce, userAgent) =>
          val versionPayloadReply = VersionPayload(state.remoteConnector).copy(nonce=nonce)
          log.info(s"Replied using $versionPayloadReply to $remoteConnector")
          state.wire ! Tcp.Write(ByteString(WiredPayload.toBytes(versionPayloadReply)))
          stay
        case v @ Pex(ids) =>
          log.info(s"Got $ids from ${state.remoteConnector}")
          state.wire ! Tcp.Write(ByteString(WiredPayload.toBytes(Pex(Set.empty))))
          stay

      }
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
      case None => stop(Failure(s"Expected payload, got $bytes"))
    }
  }

  def parse(byteString: ByteString)(f: PartialFunction[Payload, State]): State = parse(byteString.toArray)(f)
}

object PeerConnection {
  sealed trait Protocol
  case class OutgoingConnection(tcp: ActorRef, remote: Connector, local: Connector) extends Protocol
  case class IncomingConnection(tcp: ActorRef, remote: Connector, local: Connector) extends Protocol
  case class Received(bytes: Array[Byte]) extends Protocol

  sealed trait State
  case object Initial extends State
  case object Connected extends State
  case object WaitingForVersionPayloadReply extends State

  sealed trait Data
  case object NoData extends Data
  case class ConnectionData(wire: ActorRef, remoteConnector: Connector, localConnector: Connector) extends Data
  case class WaitingForVersionPayloadData(nonce: Long, connectionData: ConnectionData) extends Data

  def props: Props = Props(classOf[PeerConnection])
}
