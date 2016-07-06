import akka.actor._
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity._
import com.machinomy.xicity.transport._

implicit val system = ActorSystem()
val identifier = Identifier(33)
println(identifier)


val node = system.actorOf(Node.props(identifier))
val nodeWrap = Node.Wrap(node, Parameters.default)
val c = system.actorOf(ClientNode.props(nodeWrap))
/// Wait for some time until PEX is done
//c ! Message.Shot(identifier, Identifier(12), 0, "Hello".getBytes, DateTime.now.getMillis / 1000 + 300)
c ! Message.Shot(identifier, Identifier(10), 0, "Hello from the other side".getBytes, DateTime.now.getMillis / 1000 + 10*60)
