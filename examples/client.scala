import akka.actor._
import com.github.nscala_time.time.Imports._
import com.machinomy.xicity._
import com.machinomy.xicity.mac._
import com.machinomy.xicity.network.{ClientNode, Kernel}

implicit val system = ActorSystem()
val identifier = Identifier(10)
println(identifier)


val node = system.actorOf(Kernel.props(identifier))
val nodeWrap = Kernel.Wrap(node, Parameters.default)
val c = system.actorOf(ClientNode.props(nodeWrap))
/// Wait for some time until PEX is done
//c ! Message.Shot(identifier, Identifier(12), 0, "Hello".getBytes, DateTime.now.getMillis / 1000 + 300)
c ! Message.MultiShot(identifier, Set(Identifier(12), Identifier(14)), 0, "Hello".getBytes, DateTime.now.getMillis / 1000 + 300)
