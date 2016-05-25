import akka.actor.ActorSystem
import com.machinomy.xicity.{Connector, Identifier, PeerNode}

object PeerNodeStarter {
  def main() = {
    val system = ActorSystem()
    val identifier = Identifier.random
    val peerNode = system.actorOf(PeerNode.props(identifier))
    peerNode ! PeerNode.StartServerCommand(Connector("localhost"))
  }
}
