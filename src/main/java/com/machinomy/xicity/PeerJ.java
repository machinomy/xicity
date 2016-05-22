package com.machinomy.xicity;

import net.tomp2p.connection.Bindings;
import net.tomp2p.dht.*;
import net.tomp2p.p2p.*;
import net.tomp2p.futures.FutureBootstrap;
import net.tomp2p.futures.FutureDiscover;
import net.tomp2p.p2p.builder.DiscoverBuilder;
import net.tomp2p.peers.Number160;
import net.tomp2p.rpc.ObjectDataReply;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

public class PeerJ {
    public final int PORT = 9333;

    private String[] seeds;
    private Number160 address;
    private net.tomp2p.p2p.Peer peer;
    private boolean isConnected = false;
    private PeerDHT peerDHT;

    public PeerJ(String[] seeds, Number160 address) {
        this.seeds = seeds;
        this.address = address;
    }

    public boolean start() throws IOException {
        Bindings bindings = new Bindings().listenAny();
        this.peer = new PeerBuilder(address).ports(PORT).bindings(bindings).start();
        return bootstrap();
    }

    public boolean bootstrap() {
        boolean success = false;
        InetAddress masterInetAddress;
        FutureDiscover futureDiscover;
        FutureBootstrap futureBootstrap;
        for (String seed : this.seeds) {
            if (!success) {
                try {
                    System.out.println(seed);
                    masterInetAddress = InetAddress.getByName(seed);
                    System.out.println(masterInetAddress);
                    DiscoverBuilder a = this.peer.discover();
                    DiscoverBuilder b = a.expectManualForwarding();
                    DiscoverBuilder c = b.inetAddress(masterInetAddress);
                    DiscoverBuilder d = c.ports(PORT);
                    futureDiscover = d.start();
                    futureDiscover.awaitUninterruptibly();
                    futureBootstrap = peer.bootstrap().inetAddress(masterInetAddress).ports(PORT).start();
                    futureBootstrap.awaitUninterruptibly();
                    if (futureDiscover.isSuccess() && futureBootstrap.isSuccess()) {
                        success = true;
                        this.isConnected = true;
                        this.peerDHT = new PeerBuilderDHT(this.peer).start();
                    }
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }
        return success;
    }

    public FutureSend send(Number160 receiver, Object something) {
        RequestP2PConfiguration requestP2PConfiguration = new RequestP2PConfiguration(1, 10, 0);
        return peerDHT.send(receiver).object(something).requestP2PConfiguration(requestP2PConfiguration).start();
    }

    public void receive(ObjectDataReply dataReply) {
        peerDHT.peer().objectDataReply(dataReply);
    }
}
