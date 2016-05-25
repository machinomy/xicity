import akka.actor.ActorSystem
import com.machinomy.xicity.{Connector, Identifier, PeerNode}

object PeerNodeClientStarter {
  def main() = {
    val system = ActorSystem()
    val peerNode = system.actorOf(PeerNode.props(Identifier.random))
    val seeds = Set(Connector("localhost"), Connector("localhost", 9343))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
  }
}
