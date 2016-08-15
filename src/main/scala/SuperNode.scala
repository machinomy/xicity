import akka.actor.ActorSystem
import akka.util.Timeout
import com.machinomy.xicity.Identifier
import com.machinomy.xicity.transport.{Address, Node, Parameters, ServerNode}

import scala.concurrent.duration.FiniteDuration

object SuperNode extends App {
  implicit val system = ActorSystem()
  val identifier = Identifier.random
  println(s"Using identifier: $identifier")
  val nodeActor = system.actorOf(Node.props(identifier))
  val parameters = new Parameters {
    override def threshold: Byte = Parameters.default.threshold
    override def tickInterval: FiniteDuration = Parameters.default.tickInterval
    override def tickInitialDelay: FiniteDuration = Parameters.default.tickInitialDelay
    override def serverAddress: Address = Address("127.0.0.1")
    override def seeds: Set[Address] = Set.empty
    override def timeout: Timeout = Parameters.default.timeout
    override def port: Int = Parameters.default.port
  }
  val serverNodeActor = system.actorOf(ServerNode.props(Node.Wrap(nodeActor, parameters)))
}
