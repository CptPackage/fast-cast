fast-cast
=========


High performance low latency topic/stream based reliable UDP messaging ("event-bus").

**3.x** is in the making. Old remote method layer has been abandonned (will be covered by future konktraktor releases)

**3.0 features**:
- Throughput up to **7 million 70 bytes msg/second** (Intel i7 or newer XEONS, 10GB network or localhost).
- **reliable low latency with extraordinary few outliers**. Testscenario: Ping-Pong RTT latency. XEON 3Ghz, CentOS 6.5 RT Linux: RTT latency mean:12 micros, 99.9% - 24 micros, 99.99% - 111 micros, 99.9999% - 126 micros. 
- transparent fragmentation and defragmentation of **large messages** (max 50% of publisher history buffer and < subscribers's receive buffer).
- **add hoc unicast** (publisher can address all subscribers or a single subscriber on a per message level).
- supports **fully reliable** as well as unreliable streams (unordered-reliable streams coming soon)
- **blocking IO** (saves CPU) and **lock free poll** mode (low latency, CPU/cores burned)
- all buffers are kept **off heap** to avoid GC pressure.
- **allocation free** in the main path
- requires **JDK 1.7** or higher

check out examples folder and tests on how to use fc. Documentation pending .. this is beta software

initial release is available on maven.
```
<dependency>
    <groupId>de.ruedigermoeller</groupId>
    <artifactId>fast-cast</artifactId>
    <version>3.03</version>
</dependency>
```

Changes done from 2.x to 3.x:
- removed remote method framework completely (will be replaced by kontraktor actors on top of fast-cast). This will  reduce exposure to bugs and also reduces impl complexity.
- refurbished+refactored core NAK UDP streaming implementation.
- simplified API
- 3.0 has been optimized for low latency (2.x is a bastard latency wise ..). 
- requires fast-serialization 2.17 branch build for struct support
- allocation free under normal operation

==ShortDoc==

**Multicast**

Fastcast uses ip4 multicast. This means a sender can sends one packet, which is then received by all subscribers. This can be advantageous e.g. for high avaiability or broadcasting of common state changes across a cluster of processes. Multicast cluster scale much better than connection based tcp clusters, as instead of sending stuff twice on several connections, its only put once onto the network.

Multicast addresses start at 224.0.0.0, however its recommended to use addresses > 225.0.0.0. Do not on rely on address, its more important which port is chosen (avoid "crosstalking").
With increasingily defensive configuration defaults, getting multicast to run on a network can be pretty time consuming. The following things are usually problematic:
* rp_filter of linux kernel (reverse filtering fails because multicast packet can have weird sender address). E.g. RH7
* firewall defaults
* disabled at network adapter level
* traffic shaping switches defaulting to allow limited bandwidth for multicast traffic
* complex network setups with slow network segments attached might backpressure multicast traffic accross the whole network. E.g. an attached 100MBit or wireless lan segment might cause multicast traffic in the 1GBit lan to slow down to wireless network speed.

ethtool, tcpdump, and netstat are your diagnostic helpers ..

**Reliability Algorithm used by fast-cast**

Fastcast employs pure NAK. A ublisher keeps a history of packets sent. A sender keeps a sequence for packets sent. A subscriber keeps a sequence per publisher (so multiple publishers on same topic/adddr:port are supported) and a receive buffer per publisher.
Once the publisher detects a gap it waits a short time if the gap fills (e.g. just reordered packet). If it does not get filled it sends a retransmission broadcast (targeted to the sender id). The sender then resends the missing packet(s). Once the receiver can close the gap, receiving can be continued. Packets received while retransmission request is in flight, are buffered, so in case the missing packet arrives, buffered packets usually allow for further processing without new gaps.
So two buffer sizes are important:
- history buffer (num_datagrams) of publisher
- receive buffer (num_datagrams) of subscriber
The higher the throughput and the higher you expect processes to stall (e.g. GC) the larger the publisher history buffer must be sized.
The higher the thorughput and the higher the latency of your network, the higher the receive buffer must be sized (receive buffer should be able to buffer number of packets sent while a retransmission request/response is in flight). As retransmission requests implicitely lower the send rate of publisher, a slightly too low setting of receive buffers maight hamper throughput in case packet loss occurs, its not that critical for overall stability.
Once a publisher overruns a subscriber such that the subscriber wants a retransmission on packet which is already out of the senders history ring buffer, the subscriber gets a signal (see subscriber interfac) that it cannot recover the requested messages. Message loss has happened.

**Flow control**

Fast cast is configured by plain limit rating (number of "packets" [datagrams] per second). However retransmission responses sent by a publisher implicitely lower its send rate.

**Batching**

The message send call ("offer") has flag determining wether the data should be sent immediately (flush) or if batching should be applied. If  'flush' is choosen and no further message is offered, an automatic flush will be triggered after (setting) some milliseconds. If 'flush' is set to true and the publisher is near its packet rate limit, batching will be applied. This way one can achieve that low rate traffic is sent with low latency, however once traffic bursts occur, batching will avoid backpressure onto publisher.

**Packet size**

With packet actually a fast-cast 'datagram' size is meant. For lowest latency choose a packet size slightly lower than netork MTU. For high throughput choose larger packet sizes (up to 65k). Downside of large packet sizes is, that a packet gap has worse effects (because e.g. 64k need to be retransmitted instead of just 1k). As history and receive buffers reserve N*full packet size number of bytes, large packets also increase required memory to hold buffers. Its good practice to choose multiples of MTU for packet sizes, though its not that significant. Usual values are 1.5k, 3k, 8k, 16k . 64k are also a possible setting (but large buffers).

**large messages**

Large messages are automatically fragmented/defragmented. A message cannot be larger than a subscribers receive buffer, and not larger than a publishers send history (give at least 10%-20% headrooom).
Expect serious throughput hiccups with very large messages (>40MB and higher), especially if processes have been started and are not yet warmed up (JIT optimization hasn't kicked in yet). Once hotspot has warmed up code, even 80MB messages might pass smoothly.

**configuration recommendation**

start with low packet rate and moderate packet size (e.g. 8k). History buffer should cover at least 3-5 seconds (java JIT hiccups on newly joining processes, GC). E.g. packet send rate = 5000, 8k buffers => history for 5 seconds = 5*5000 = 25000 = (multiplied with packet size) 200MB. Receivebuffer ~1-2 seconds of traffic = 10_000 packets.
**Ensure subscribers do not block the receiving thread.**

**API**

see API package + examples project directory.

**Terminology**

A 'Transport' is a multicast address+port. A transport can be divided into up to 256 topics. A publishers sends on TopicId:transport. Note that topic traffic is still received and filtered by fast cast. So for high throuput or low latency topics its recommended to use a dedicated transport (filtering done by network hardware then). Note this can be easily changed at config level, so for dev you might want to use one transport (mcast-addr:port). In production you prefer a dedicated transport per topic.

**NodeId, Unicast**

Each node is assigned a unique id. If null is provided as a receiver in the offer method, all subscribers will receive the message sent. If a nodeid is provided, only the specific node will receive the packet. Note that if one alternates quickly in between receiverIds or 'null' then 'nodeId', batching might suffer, as the receiver id is set on packet level, not message level.
