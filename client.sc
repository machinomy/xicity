import java.net.{Inet4Address, InetAddress}
import java.util.Random

import net.tomp2p.connection.{Bindings, DefaultConnectionConfiguration, DiscoverNetworks}
import net.tomp2p.p2p.PeerBuilder
import net.tomp2p.peers.{Number160, PeerAddress}

import scala.collection.JavaConversions._

val PORT = 9333
val WELL_KNOWN_IP = "192.168.50.4"

val number = new Number160(new Random())
val bindings = new Bindings().listenAny()
val client = new PeerBuilder(number).ports(PORT).bindings(bindings).start()
println(s"Client started, listening to ${DiscoverNetworks.discoverInterfaces(bindings)}")
println(s"Outside address is ${client.peerAddress()}")

val masterInetAddress = InetAddress.getByName(WELL_KNOWN_IP)
val masterAddress = new PeerAddress(Number160.ZERO, masterInetAddress, PORT, PORT + 1)
println(s"master address: $masterAddress")

val futureDiscover = client.discover.expectManualForwarding.inetAddress(masterInetAddress).ports(PORT).start
futureDiscover.awaitUninterruptibly()

val futureBootstrap = client.bootstrap.inetAddress(masterInetAddress).ports(PORT).start
futureBootstrap.awaitUninterruptibly()

val addresses = client.peerBean().peerMap().all()
println(s"${addresses.toString}")

if (futureDiscover.isSuccess) {
  println("found that my outside address is " + futureDiscover.peerAddress)
} else {
  println("failed " + futureDiscover.failedReason)
}
client.shutdown()
