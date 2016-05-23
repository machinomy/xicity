import java.util.Random

import akka.actor.Status.Failure
import com.machinomy.xicity.{Peer, PeerJ}
import net.tomp2p.peers.{Number160, PeerAddress}
import net.tomp2p.rpc.ObjectDataReply

import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._
import scala.util.Success

// checkPeerA.main(Array.empty)
object checkPeerA extends App {
  val selfNumber = new Number160(0xfa)
  val maybePeer = Peer.build(selfNumber)
  maybePeer.onSuccess { case peer =>
    val message = "Hello"
    println(s"Sending $message")
    val a = peer.request(new Number160(0xfb), message.getBytes).getOrElse(Array.empty)
    val receivedString = a.map(_.toChar).mkString
    println(s"Received $receivedString")
  }
}
