import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import com.machinomy.xicity.connectivity.Address

object PeerNodeStarter {
  def main() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Identifier, PeerNode}
    val system = ActorSystem()

    class Logic extends Actor with ActorLogging {
      var peerNodeRef: ActorRef = null
      override def receive: Receive = {
        case PeerNode.DidStart(n) =>
          peerNodeRef = n
          log info "DID START"
        case msg @ PeerNode.ReceivedSingleMessage(from, to, text, expiration) =>
          log debug msg.toString
        case e => log.error(e.toString)
      }
    }

    val logic = system.actorOf(Props(classOf[Logic]))
    println(logic)
    val identifier = Identifier.random
    println(identifier)
    val peerNode = system.actorOf(PeerNode.props(identifier, logic))
    peerNode ! PeerNode.StartServerCommand(Address("0.0.0.0"))
    /*val seeds = Set(Address("45.55.122.116"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
    peerNode ! PeerNode.SendSingleMessageCommand(identifier, new Identifier(BigInt(34)), "foo".getBytes, DateTime.now.getMillis / 1000 + 5)*/
  }
}
