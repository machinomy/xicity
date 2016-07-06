import com.machinomy.xicity._
import com.machinomy.xicity.transport._
import akka.actor._

implicit val system = ActorSystem()
val identifier = Identifier(12)
println(identifier)

class Peer extends Actor with ActorLogging {
  var nodeActor: ActorRef = null
  var serverNodeActor: ActorRef = null

  override def preStart(): Unit = {
    nodeActor = context.actorOf(Node.props(identifier, self))
    serverNodeActor = context.actorOf(ServerNode.props(Node.Wrap(nodeActor, Parameters.default)))
  }

  override def receive: Receive = {
    case Peer.IsReady() =>
      log.info("NODEISREADY")
    case m: Message.Shot =>
      log.info(s"RECEIVEDSHOT $m")
  }

  override def postStop(): Unit = {
    context.stop(nodeActor)
  }
}

val peer = system.actorOf(Props(classOf[Peer]))
