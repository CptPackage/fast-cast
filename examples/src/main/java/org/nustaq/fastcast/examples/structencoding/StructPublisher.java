package org.nustaq.fastcast.examples.structencoding;

import org.nustaq.fastcast.api.FCPublisher;
import org.nustaq.fastcast.api.FastCast;
import org.nustaq.fastcast.config.PhysicalTransportConf;
import org.nustaq.fastcast.config.PublisherConf;
import org.nustaq.fastcast.api.util.ObjectPublisher;
import org.nustaq.fastcast.util.RateMeasure;
import org.nustaq.offheap.structs.FSTStructAllocator;

import java.util.concurrent.ThreadLocalRandom;

/**
 * Created by moelrue on 12/15/14.
 *
 * Demonstrates high throughput (up to 7 million msg/second) allocation free messaging
 * using struct encoding.
 *
 */
public class StructPublisher {

    public static void main(String arg[]) {

        FastCast.getFastCast().setNodeId("PUB"); // 5 chars MAX !!
        configureFastCast();

        FCPublisher pub = FastCast.getFastCast().onTransport("default").publish(
                new PublisherConf(1)            // unique-per-transport topic id
                    .numPacketHistory(33_000)   // how many packets are kept for retransmission requests
                    .pps(10_000)                // packets per second rate limit.
        );

        Protocol.initStructFactory();

        Protocol.PriceUpdateStruct template = new Protocol.PriceUpdateStruct();
        FSTStructAllocator onHeapAlloc = new FSTStructAllocator(0);

        Protocol.PriceUpdateStruct msg = onHeapAlloc.newStruct(template); // speed up instantiation

        ThreadLocalRandom current = ThreadLocalRandom.current();
        // could directly send raw on publisher
        RateMeasure measure = new RateMeasure("msg/s");
        while( true ) {
            measure.count();

            // fill in data
            Protocol.InstrumentStruct instrument = msg.getInstrument();
            instrument.getMnemonic().setString("BMW");
            instrument.setInstrumentId(13);
            msg.setPrc(99.0+current.nextDouble(10.0)-5);
            msg.setQty(100+current.nextInt(10));

            // send message
            while( ! pub.offer(null,msg.getBase(),msg.getOffset(),msg.getByteSize(),false) ) {
                /* spin */
            }

        }
    }

    public static void configureFastCast() {
        // note this configuration is far below possible limits regarding throughput and rate
        FastCast fc = FastCast.getFastCast();
        fc.addTransport(
                new PhysicalTransportConf("default")
                        .interfaceAdr("127.0.0.1")  // define the interface
                        .port(42043)                // port is more important than address as some OS only test for ports ('crosstalking')
                        .mulitcastAdr("229.9.9.9")  // ip4 multicast address
                        .setDgramsize(64_000)         // datagram size. Small sizes => lower latency, large sizes => better throughput [range 1200 to 64_000 bytes]
                        .socketReceiveBufferSize(4_000_000) // as large as possible .. however avoid hitting system limits in example
                        .socketSendBufferSize(2_000_000)
                        // uncomment this to enable spin looping. Will increase throughput once datagram size is lowered below 8kb or so
//                        .idleParkMicros(1)
//                        .spinLoopMicros(100_000)
        );

    }
}
