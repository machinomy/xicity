package com.machinomy.xicity.playground

import akka.actor.{Actor, ActorContext, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.{Message, Parameters}
import com.machinomy.xicity.network.{FullNode, Peer, PeerBase}
import scodec._
import scodec.bits.{ByteVector, _}
import scodec.codecs._

import scala.util.{Failure, Success}

object SenderApp extends App {
  class Node(identifier: Identifier, parameters: Parameters, postReady: (ActorContext, Parameters) => Unit) extends Actor with ActorLogging {
    private var upstream: ActorRef = _
    private var track: Map[Int, ActorRef] = Map.empty[Int, ActorRef]

    override def preStart(): Unit = {
      upstream = context.actorOf(PeerBase.props[FullNode](identifier, self, parameters), s"peer-${identifier.number}")
    }

    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"AAA I am Ready as $identifier")
        postReady(context, parameters)
      case Peer.Received(from, text, expiration) =>
        val textString: String = text.map(_.toChar).mkString
        log.info(s"AAA Received $textString from $from")
        Codec.decode[Node.Request](ByteVector(text).toBitVector).toOption match {
          case Some(decodeResult) =>
            val request = decodeResult.value
            val responseText = "EHLO".getBytes
            val response = Node.Response(responseText, request.id)
            val responseBytes = Codec.encode(response).toOption.get.toByteArray
            upstream ! Message.Single(identifier, from, responseBytes, expiration)
          case None =>
            log.info(s"AAA Received anything but Node.Request")
        }
        Codec.decode[Node.Response](ByteVector(text).toBitVector).toOption match {
          case Some(decodeResult) =>
            val response = decodeResult.value
            track.get(response.id) match {
              case Some(actorRef) =>
                actorRef ! response
                track -= response.id
                val responseText = response.text.toList.mkString
                log.info(s"AAA GOT RESPONSE!!!!: $responseText")
              case None =>
                log.info(s"AAA Nowhere to send response ${response.id}")
            }
          case None => log.info(s"AAA Received anything but Node.Response")
        }
      case request @ Node.Request(receiver, text, id) =>
        for {
          requestBytes <- Codec.encode(request).toOption.map(_.toByteArray)
        } {
          track += (id -> sender)
          val expiration = DateTime.now.getMillis / 1000 + 10
          upstream ! Message.Single(identifier, receiver, requestBytes, expiration)
        }
      case something =>
        println(s"I am $identifier, got $something")
        upstream ! something
    }

    override def postStop(): Unit = {
      context.stop(upstream)
    }

    val identifierCodec = Message.identifierCodec
    val textCodec = variableSizeBytes(int32L, bytes)
    val idCodec = int32L

    implicit val requestCodec = new Codec[Node.Request] {
      override def decode(bits: BitVector): Attempt[DecodeResult[Node.Request]] =
        for {
          identifierR <- identifierCodec.decode(bits)
          identifier = identifierR.value
          textR <- textCodec.decode(identifierR.remainder)
          text = textR.value.toArray
          idR <- idCodec.decode(textR.remainder)
          id = idR.value
        } yield DecodeResult(Node.Request(identifier, text, id), idR.remainder)

      override def encode(value: Node.Request): Attempt[BitVector] =
        for {
          receiverBytes <- identifierCodec.encode(value.receiver)
          textBytes <- textCodec.encode(ByteVector(value.text))
          idBytes <- idCodec.encode(value.id)
        } yield receiverBytes ++ textBytes ++ idBytes

      override def sizeBound: SizeBound =
        identifierCodec.sizeBound + textCodec.sizeBound + int32L.sizeBound
    }

    implicit val codec = new Codec[Node.Response] {
      override def decode(bits: BitVector): Attempt[DecodeResult[Node.Response]] =
        for {
          textR <- textCodec.decode(bits)
          text = textR.value.toArray
          idR <- idCodec.decode(textR.remainder)
          id = idR.value
        } yield DecodeResult(Node.Response(text, id), idR.remainder)

      override def encode(value: Node.Response): Attempt[BitVector] =
        for {
          textBytes <- textCodec.encode(ByteVector(value.text))
          idBytes <- idCodec.encode(value.id)
        } yield textBytes ++ idBytes

      override def sizeBound: SizeBound = textCodec.sizeBound + idCodec.sizeBound
    }
  }

  object Node {
    case class Request(receiver: Identifier, text: Array[Byte], id: Int)
    case class Response(text: Array[Byte], id: Int)
  }

  val system = ActorSystem("xicity")
  val identifierB = Identifier(100)
  val identifierA = Identifier(200)

  def postReadyA(context: ActorContext, parameters: Parameters): Unit = {
    implicit val timeout = parameters.timeout
    implicit val ec = context.dispatcher
    val response = (context.self ? Node.Request(identifierB, "HELLO".getBytes, 10)).mapTo[Node.Response]
    response.onComplete {
      case Success(foo) =>
        println(s"AAA Received response: $foo")
      case Failure(e) =>
        println(s"AAA Received error: $e")
    }
  }

  def postReadyB(context: ActorContext, parameters: Parameters): Unit = {
    println(s"AAA NodeB started")
  }

  val nodeB = system.actorOf(Props(classOf[Node], identifierB, Parameters.default, postReadyB _))

  val nodeA = system.actorOf(Props(classOf[Node], identifierA, Parameters.default, postReadyA _))
}
