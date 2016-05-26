import akka.actor.ActorSystem
import com.machinomy.xicity.{Connector, Identifier, PeerNode}

object PeerNodeClientStarter {
  def main() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}


    val system = ActorSystem()
    val identifier = Identifier.random
    val peerNode = system.actorOf(PeerNode.props(identifier))
    val seeds = Set(Connector("localhost"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
    val cmd = PeerNode.SendSingleMessageCommand(identifier, new Identifier(BigInt("8833507116264057024")), "foo".getBytes)
    // peerNode ! cmd
    // 9075988430028513838
  }
}
