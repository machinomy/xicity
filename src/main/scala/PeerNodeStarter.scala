object PeerNodeStarter {
  def main() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}
    val system = ActorSystem()
    val peerNode = system.actorOf(PeerNode.props(Identifier.random))
    peerNode ! PeerNode.StartServerCommand(Connector("0.0.0.0"))
    val seeds = Set(Connector("45.55.122.116"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
  }
}
