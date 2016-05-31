import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import com.machinomy.xicity.{Connector, Identifier, PeerNode}
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity.protocol.Payload

object PeerNodeClientStarter {
  def main() = {
    import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}
    import com.github.nscala_time.time.Imports._

    val identifier = new Identifier(34)

    class Logic extends Actor with ActorLogging {
      var peerNodeRef: ActorRef = null
      override def receive: Receive = {
        case PeerNode.DidStart(n) =>
          peerNodeRef = n
        case cmd @ PeerNode.ReceivedSingleMessage(from, to, text, expiration) =>
          log info "RECEVIEDRECEIVED"
          log info cmd.toString
        case e => log.error(e.toString)
      }
    }

    val system = ActorSystem()

    val logic = system.actorOf(Props(classOf[Logic]))
    val peerNode = system.actorOf(PeerNode.props(identifier, logic))
    val seeds = Set(Connector("0.0.0.0"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)

    val msg = PeerNode.SendSingleMessageCommand(
      from = identifier,
      to = Identifier(BigInt("3708120315101760066")),
      text = s"HELLO".getBytes,
      expiration = DateTime.now.getMillis / 1000 + 6000
    )
    peerNode ! msg
    for (i <- 1 to 10) {
      peerNode ! PeerNode.SendSingleMessageCommand(
        from = identifier,
        to = Identifier(BigInt("3708120315101760066")),
        text = s"HELLO".getBytes,
        expiration = DateTime.now.getMillis / 1000 + 600
      )
    }
  }

  def nodeB() = {
    import akka.actor.ActorSystem
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}
    import com.github.nscala_time.time.Imports._
    val system = ActorSystem()

    class Logic extends Actor with ActorLogging {
      var node: ActorRef = null
      override def receive: Receive = {
        case PeerNode.DidStart(n) =>
          this.node = n
          log debug "DID START"
        case msg @ PeerNode.ReceivedSingleMessage(from, to, text, expiration) =>
          log debug msg.toString
        case e => log.error(e.toString)
      }
    }
    val logic = system.actorOf(Props(classOf[Logic]))
    val identifier = Identifier(100)
    val peerNode = system.actorOf(PeerNode.props(identifier, logic))
    val seeds = Set(Connector("45.55.122.116"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
    val cmd = PeerNode.SendSingleMessageCommand(identifier, new Identifier(34), "foo".getBytes, DateTime.now.getMillis / 1000 + 5)
    peerNode ! cmd
    // 9075988430028513838
  }

  def nodeC() = {
    import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
    import com.machinomy.xicity.{Connector, Identifier, PeerNode}
    import com.github.nscala_time.time.Imports._

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

    val system = ActorSystem()
    val identifier = new Identifier(0xfa)
    val logic = system.actorOf(Props(classOf[Logic]))
    val peerNode = system.actorOf(PeerNode.props(identifier, logic))
    val seeds = Set(Connector("localhost"))
    peerNode ! PeerNode.StartClientsCommand(2, seeds)
  }
}
