import com.github.nscala_time.time.Imports._

object PeerNodeStarter {
  def main() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}
    val system = ActorSystem()
    val identifier = Identifier.random
    def receiver(from: Identifier, to: Identifier, msg: Array[Byte], exp: Long) = {
      println("µµµµµµµµµµµ")
    }
    val peerNode = system.actorOf(PeerNode.props(identifier, receiver))
    peerNode ! PeerNode.StartServerCommand(Connector("0.0.0.0"))
    val seeds = Set(Connector("45.55.122.116"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
    peerNode ! PeerNode.SendSingleMessageCommand(identifier, new Identifier(BigInt(34)), "foo".getBytes, DateTime.now.getMillis / 1000 + 5)
  }
}
