package com.machinomy.xicity.connectivity

import akka.actor.{Actor, ActorLogging, ActorRef, ActorRefFactory, Props}
import akka.io.{IO, Tcp}
import akka.pattern._
import akka.util.Timeout

import scala.concurrent.{ExecutionContext, Future}

case class IncomingConnectionFactory(listener: ActorRef, running: Boolean = false)

object IncomingConnectionFactory {
  class ListenerA(local: Endpoint, handlerFactory: Vertex => ActorRef) extends Actor with ActorLogging {
    import context.system

    override def receive: Receive = {
      case ListenerA.Start =>
        IO(Tcp) ! Tcp.Bind(self, local.address)
        log.info(s"Binding to $local...")
        val master = sender()
        context become {
          case Tcp.Bound(localBound) =>
            log.info(s"Bound to $localBound")
            val wire = sender()
            master ! ListenerA.DidStart
            context become {
              case Tcp.Connected(remoteAddress, localAddress) =>
                val remote = Endpoint(remoteAddress)
                val vertex = Vertex(remote, wire)
                log info s"Got incoming connection from $remote"
                val handler = handlerFactory(vertex)
                sender ! Tcp.Register(handler)
                handler ! Tcp.Connected(remoteAddress, localAddress)
              case ListenerA.Stop =>
                wire ! Tcp.Close
                sender() ! ListenerA.DidStop
                stop()
            }
          case Tcp.CommandFailed(cmd: Tcp.Bind) =>
            log.error(s"Can not bind to ${cmd.localAddress}")
            stop()
        }
      case ListenerA.Stop =>
        sender ! ListenerA.DidStop
        stop()
    }

    def stop() = {
      log info s"Shutting down the listener"
      context stop self
    }
  }

  object ListenerA {
    sealed trait Protocol
    object Start extends Protocol
    object DidStart extends Protocol
    object Stop extends Protocol
    object DidStop extends Protocol
    def props(local: Endpoint, handlerFactory: Vertex => ActorRef) = Props(classOf[ListenerA], local, handlerFactory)
  }

  def build(local: Endpoint, behavior: Behavior)(implicit af: ActorRefFactory, t: Timeout, ec: ExecutionContext): Future[IncomingConnectionFactory] = {
    val handlerFactory = (vertex: Vertex) => af.actorOf(Connection.props(vertex, behavior))
    val listener = af.actorOf(ListenerA.props(local, handlerFactory))
    val factory = new IncomingConnectionFactory(listener)
    Future.successful(factory)
  }

  def start(incomingConnectionFactory: IncomingConnectionFactory)(implicit t: Timeout, ec: ExecutionContext): Future[IncomingConnectionFactory] = {
    (incomingConnectionFactory.listener ? ListenerA.Start).mapTo[ListenerA.DidStart.type].map {
      case ListenerA.DidStart => incomingConnectionFactory.copy(running = true)
      case _ => incomingConnectionFactory
    }
  }

  def stop(incomingConnectionFactory: IncomingConnectionFactory)(implicit t: Timeout, ec: ExecutionContext): Future[IncomingConnectionFactory] = {
    (incomingConnectionFactory.listener ? ListenerA.Stop).mapTo[ListenerA.DidStop.type].map {
      case ListenerA.DidStop => incomingConnectionFactory.copy(running = false)
      case _ => incomingConnectionFactory
    }
  }
}
