import java.util.Random

import com.machinomy.xicity.{Peer, PeerJ}
import net.tomp2p.peers.{Number160, PeerAddress}
import net.tomp2p.rpc.ObjectDataReply

import scala.concurrent.ExecutionContext.Implicits.global

// checkPeerB.main(Array.empty)
object checkPeerB extends App {
  val maybePeer = Peer.build(new Number160(0xfb))
  maybePeer.onSuccess { case peer =>
    peer.reply { (sender, bytes) =>
      "WORD".getBytes
    }
  }
  maybePeer.onComplete { case y => println(y) }
}
