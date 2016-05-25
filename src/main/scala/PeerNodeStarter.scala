import akka.actor.ActorSystem
import com.machinomy.xicity.{Connector, Identifier, PeerNode}

object PeerNodeStarter {
  def main() = {
    val system = ActorSystem()
    val peerNode = system.actorOf(PeerNode.props(Identifier.random))
    peerNode ! PeerNode.StartServerCommand(Connector("localhost"))

    val peerNode2 = system.actorOf(PeerNode.props(Identifier.random))
    peerNode2 ! PeerNode.StartServerCommand(Connector("localhost", 9343))
  }
}
