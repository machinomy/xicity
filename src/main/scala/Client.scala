import akka.actor.ActorSystem
import com.machinomy.xicity.{Connector, PeerClient, PeerConnection}

object Client {
  def main() = {
    val connector = Connector("localhost")
    val system = ActorSystem()
    val handler = system actorOf PeerConnection.props
    val client = system.actorOf(PeerClient.props(connector, handler))

    client ! PeerClient.StartCommand
  }
}
