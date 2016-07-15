import akka.actor._
import com.machinomy.xicity._
import com.machinomy.xicity.mac._
import com.machinomy.xicity.network.{Kernel, Peer, ServerNode}

implicit val system = ActorSystem()
val identifier = Identifier(12)
println(identifier)

class Peer extends Actor with ActorLogging {
  var nodeActor: ActorRef = null
  var serverNodeActor: ActorRef = null

  override def preStart(): Unit = {
    nodeActor = context.actorOf(Kernel.props(identifier, self))
    serverNodeActor = context.actorOf(ServerNode.props(Kernel.Wrap(nodeActor, Parameters.default)))
  }

  override def receive: Receive = {
    case Peer.IsReady() =>
      log.info("NODEISREADY")
    case m: Message.Single =>
      log.info(s"RECEIVEDSHOT $m")
  }

  override def postStop(): Unit = {
    context.stop(nodeActor)
  }
}

val peer = system.actorOf(Props(classOf[Peer]))
