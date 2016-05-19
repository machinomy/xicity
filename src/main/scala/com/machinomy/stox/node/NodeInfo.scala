package com.machinomy.stox.node

import java.net.InetSocketAddress
import com.machinomy.stox.sodium.PublicKey

case class NodeInfo(protocol: Transport.Protocol, address: InetSocketAddress, publicKey: PublicKey)
