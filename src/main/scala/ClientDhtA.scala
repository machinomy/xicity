import java.net.InetAddress

import net.tomp2p.connection.{Bindings, DiscoverNetworks}
import net.tomp2p.dht.{FutureSend, PeerBuilderDHT, PeerDHT}
import net.tomp2p.futures.{FutureBootstrap, FutureDiscover}
import net.tomp2p.p2p.{Peer, PeerBuilder, RequestP2PConfiguration}
import net.tomp2p.peers.{Number160, PeerAddress}

import scala.collection.JavaConversions._

// ClientDhtA.main(Array.empty)
object ClientDhtA extends App {
  val PORT = 9333
  val WELL_KNOWN_IP = "192.168.50.4"

  val number = new Number160(0xFA)
  val bindings: Bindings = new Bindings().listenAny()
  val client: Peer = new PeerBuilder(number).ports(PORT).bindings(bindings).start()
  println(s"Client started, listening to ${DiscoverNetworks.discoverInterfaces(bindings)}")
  println(s"Outside address is ${client.peerAddress()}")

  val masterInetAddress = InetAddress.getByName(WELL_KNOWN_IP)

  val futureDiscover: FutureDiscover = client.discover.expectManualForwarding.inetAddress(masterInetAddress).ports(PORT).start
  futureDiscover.awaitUninterruptibly()

  val futureBootstrap: FutureBootstrap = client.bootstrap.inetAddress(masterInetAddress).ports(PORT).start
  futureBootstrap.awaitUninterruptibly()

  val addresses = client.peerBean().peerMap().all()
  println(s"${addresses.toString}")
  Thread.sleep(2*1000)
  if (futureDiscover.isSuccess) {
    println("found that my outside address is " + futureDiscover.peerAddress)
    val dhtPeer: PeerDHT = new PeerBuilderDHT(client).start()
    //val dataToPut = s"data: ${new Random().nextInt()}"
    //println(dataToPut)
    //dhtPeer.put(new Number160(10)).data(new Data(dataToPut)).start().awaitUninterruptibly()
    val requestP2PConfiguration: RequestP2PConfiguration = new RequestP2PConfiguration(1, 10, 0)
    val futureSend: FutureSend = dhtPeer.send(new Number160(0xfb)).`object`("HELLO").requestP2PConfiguration(requestP2PConfiguration).start()
    futureSend.awaitUninterruptibly()
    if (futureSend.isSuccess) {
      println(futureSend.rawDirectData2().values().toList)
    } else {
      println("Something went wrong")
    }

    Thread.sleep(10000)
  } else {
    println("failed " + futureDiscover.failedReason)
  }
}
