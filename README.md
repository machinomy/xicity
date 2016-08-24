# Xicity

Xicity is a distributed communications protocol, and library. It is intended to be secure, fast, and low profile.

## Features
- [x] Asynchronous I/O
- [ ] Request/reply pattern
- [ ] Streaming
- [ ] Resource-optimized wire format
- [ ] Resilience

## Usage

### Client

The starting point is PeerBase class. It acts as a gateway to the network. To create an instance of peer base, pass the peer identifier, network parameters, and the callback actorRef. The latter is notified every time a message is received by the peer.

The following happens in a context of an actor:

```scala
val upstreamProps = PeerBase.props[Client](identifier, self, parameters)
val upstream = context.actorOf(upstreamProps, "peer")
```

After the peer is connected to the network, it sends a message `Peer.IsReady()` to `self`.
Now you are free to send a message upstream, or just wait for incoming messages.

```scala
class Dummy(identifier: Identifier, parameters: Parameters) extends Actor with ActorLogging {
    var upstream: ActorRef = _
    
    override def preStart(): Unit = {
      val upstreamProps = PeerBase.props[Client](identifier, self, parameters)
      upstream = context.actorOf(upstreamProps, "peer")
    }
    
    override def receive: Receive = {
      case Peer.IsReady() =>
        log.info(s"Ready to transmit messages")
      case something =>
        log info s"RECEIVED $something"
    }
}
```

One could receive `Single` message, `Request`, or `Response`. The reaction is up to you. Basic expected behaviour for a "responder" is below:

```scala
override def receive: Receive = {
  case Peer.IsReady() =>
    log.info(s"Ready to transmit messages")
  case Peer.Received(payload: Message.Meaningful) =>
    payload match {
      case message: Message.Single =>
        log info s"Received single message: $message"
      case message: Message.Request =>
        log info s"Received request: $message"
        val response = Message.Response(
          from = identifier,
          to = message.from,
          text = "RESPONSE".getBytes,
          expiration = message.expiration + 2.seconds,
          id = message.id
        )
        upstream ! response
      case message: Message.Response =>
        log info s"Received response: $message"
    }
  case something =>
    log info s"RECEIVED $something"
}
```

### Super node

Super node is a client that accepts connections on a predefined IP, and connects to other seed nodes.
`com.machinomy.xicity.examples.SeedApp` is responsible for just that. It is declared as a main class for a compiled JAR.
Whenever you run the jar, it starts the super node. To bind the node to an address, use key `-b` or `--bind`
```
$ java xicity.jar --bind 127.0.0.1
```

The SeedApp powers super nodes run by Machinomy. As you would like not to connect the node to itself, use `-x` or `--exclude` key
to exclude the corresponding addresses from the seeds. Say, you would like to run the super node at adress 45.45.45.45, and the address is a well known seed node:
```
$ java xicity.jar --bind 45.45.45.45 --exclude 45.45.45.45 
```

# Call stack

## Server

    Server(ServerBehavior(Kernel)) -> Connection(IncomingConnectionBehavior(Kernel))
    
## Client

    ClientMonitor(Kernel) -> Client(ClientBehavior(Node)) -> Connection(OutgoingConnectionBehavior(Kernel))

# 
