import akka.actor.ActorSystem
import com.machinomy.xicity.{Connector, Identifier, PeerNode}
import com.github.nscala_time.time.Imports._

object PeerNodeClientStarter {
  def receiver(from: Identifier, to: Identifier, msg: Array[Byte], exp: Long) = {}
  def main() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}


    val system = ActorSystem()
    val identifier = new Identifier(34)

    val peerNode = system.actorOf(PeerNode.props(identifier, receiver))
    val seeds = Set(Connector("45.55.122.116"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
  }

  def nodeB() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}
    import com.github.nscala_time.time.Imports._
    val system = ActorSystem()
    val identifier = Identifier(100)
    val peerNode = system.actorOf(PeerNode.props(identifier, receiver))
    val seeds = Set(Connector("45.55.122.116"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
    val cmd = PeerNode.SendSingleMessageCommand(identifier, new Identifier(BigInt(34)), "foo".getBytes, DateTime.now.getMillis / 1000 + 5)
    peerNode ! cmd
  }
}
