import java.net.InetAddress

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.mac.Parameters
import com.machinomy.xicity.network.{Peer, PeerBase, Server}

object SuperNode extends App {
  val system = ActorSystem("xicity")
  val identifier = Identifier.random

  println(s"Using identifier: $identifier")

  class Dummy extends Actor with ActorLogging {
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
    }
  }
  val dummy = system.actorOf(Props(classOf[Dummy]), "dummy")
  val serverParameters = Parameters.default.copy(
    seeds = Set.empty,
    bindAddress = Some(InetAddress.getByName("45.55.122.116"))
  )
  val peerProps = PeerBase.props[Server](identifier, dummy, serverParameters)
  system.actorOf(peerProps, "peer")
}
