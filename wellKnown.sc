import java.util.Random

import net.tomp2p.connection.{Bindings, DefaultConnectionConfiguration, DiscoverNetworks}
import net.tomp2p.p2p.PeerBuilder
import net.tomp2p.peers.Number160
import scala.collection.JavaConversions._

val PORT = 9333

val serverNumber = new Number160(new Random)
val bindings = new Bindings().listenAny()
val master = new PeerBuilder(serverNumber).ports(PORT).bindings(bindings).start()
println(s"Started listening to ${DiscoverNetworks.discoverInterfaces(bindings)}")
println(s"outside address is ${master.peerAddress()}")

while(true) {
  println("Another Iteration")
  for (peerAddress <- master.peerBean().peerMap().all()) {
    println(s"PeerAddress: $peerAddress")
    val fcc = master.connectionBean().reservation().create(1, 1)
    fcc.awaitUninterruptibly()
    val channelCreator = fcc.channelCreator()

    val futureTcpResponse = master.pingRPC().pingTCP(peerAddress, channelCreator, new DefaultConnectionConfiguration())
    futureTcpResponse.awaitUninterruptibly()

    if (futureTcpResponse.isSuccess) {
      println(s"PeerJ $peerAddress is online/tcp")
    } else {
      println(s"PeerJ $peerAddress is offline/tcp")
    }

    val futureUdpResponse = master.pingRPC().pingUDP(peerAddress, channelCreator, new DefaultConnectionConfiguration())
    futureUdpResponse.awaitUninterruptibly()
    channelCreator.shutdown()

    if (futureUdpResponse.isSuccess) {
      println(s"PeerJ $peerAddress is online/udp")
    } else {
      println(s"PeerJ $peerAddress is offline/udp")
    }
  }
  Thread.sleep(1500)
}
