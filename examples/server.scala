import akka.actor._
import com.machinomy.xicity._
import com.machinomy.xicity.mac._
import com.machinomy.xicity.network.Kernel

implicit val system = ActorSystem()
val identifier = Identifier(12)
println(identifier)

class Peer extends Actor with ActorLogging {
  var nodeActor: ActorRef = null
  var serverNodeActor: ActorRef = null

  override def preStart(): Unit = {
    nodeActor = context.actorOf(Kernel.props(identifier, self))
    serverNodeActor = context.actorOf(Server.props(Kernel.Wrap(nodeActor, Parameters.default)))
  }

  override def receive: Receive = {
    case Node.IsReady() =>
      log.info("NODEISREADY")
    case m: Message.Single =>
      log.info(s"RECEIVEDSHOT $m")
    case something =>
      log.info(s"Received $something")
  }

  override def postStop(): Unit = {
    context.stop(nodeActor)
  }
}

val peer = system.actorOf(Props(classOf[Peer]))
