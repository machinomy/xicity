import java.net.{Inet4Address, InetAddress}
import java.util.Random

import net.tomp2p.connection.{Bindings, DefaultConnectionConfiguration, DiscoverNetworks}
import net.tomp2p.dht.PeerBuilderDHT
import net.tomp2p.p2p.PeerBuilder
import net.tomp2p.peers.{Number160, PeerAddress}
import net.tomp2p.rpc.ObjectDataReply
import net.tomp2p.storage.Data

import scala.collection.JavaConversions._

val PORT = 9333
val WELL_KNOWN_IP = "192.168.50.4"

val number = new Number160(0xfb)
val bindings = new Bindings().listenAny()
val client = new PeerBuilder(number).ports(PORT).bindings(bindings).start()
println(s"Client started, listening to ${DiscoverNetworks.discoverInterfaces(bindings)}")
println(s"Outside address is ${client.peerAddress()}")

val masterInetAddress = InetAddress.getByName(WELL_KNOWN_IP)

val futureDiscover = client.discover.expectManualForwarding.inetAddress(masterInetAddress).ports(PORT).start
futureDiscover.awaitUninterruptibly()

val futureBootstrap = client.bootstrap.inetAddress(masterInetAddress).ports(PORT).start
futureBootstrap.awaitUninterruptibly()

val addresses = client.peerBean().peerMap().all()
println(s"${addresses.toString}")

Thread.sleep(3*1000)
if (futureDiscover.isSuccess) {
  println("found that my outside address is " + futureDiscover.peerAddress)
  val dhtPeer = new PeerBuilderDHT(client).start()
  dhtPeer.peer().objectDataReply(new ObjectDataReply {
    override def reply(sender: PeerAddress, request: scala.Any): AnyRef = {
      println(s"I am ${client.peerID()}, received ${request.toString} from ${sender.peerId()}")
      "WORLD"
    }
  })
  /*val dataToPut = s"data: ${new Random().nextInt()}"
  println(dataToPut)
  dhtPeer.put(new Number160(10)).data(new Data(dataToPut)).start().awaitUninterruptibly()*/
} else {
  println("failed " + futureDiscover.failedReason)
}
