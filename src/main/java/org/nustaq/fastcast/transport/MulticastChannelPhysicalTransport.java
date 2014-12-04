package org.nustaq.fastcast.transport;

import org.nustaq.fastcast.config.PhysicalTransportConf;
import org.nustaq.fastcast.util.FCLog;

import java.io.*;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.*;

/**
 * Created with IntelliJ IDEA.
 * User: moelrue
 * Date: 06.06.13
 * Time: 13:45
 * To change this template use File | Settings | File Templates.
 */
public class MulticastChannelPhysicalTransport implements PhysicalTransport {

    DatagramChannel receiveSocket;
    DatagramChannel sendSocket;
    PhysicalTransportConf conf;
    NetworkInterface iface;
    InetAddress address;
    InetSocketAddress socketAddress;

    public MulticastChannelPhysicalTransport(PhysicalTransportConf conf) {
        System.setProperty("java.net.preferIPv4Stack","true" );
        this.conf = conf;
    }

    public boolean receive(DatagramPacket pack) throws IOException {
        SocketAddress receive = receiveSocket.receive(ByteBuffer.wrap(pack.getData(), pack.getOffset(), pack.getLength()));
        if ( receive instanceof InetSocketAddress) {
            pack.setAddress(((InetSocketAddress) receive).getAddress());
        }
        return receive != null;
    }

    public boolean receive(ByteBuffer pack) throws IOException {
        SocketAddress receive = receiveSocket.receive(pack);
        return receive != null;
    }

    @Override
    public void send(DatagramPacket pack) throws IOException {
        sendSocket.send(ByteBuffer.wrap(pack.getData(), pack.getOffset(), pack.getLength()), socketAddress);
    }

    @Override
    public void send(byte[] bytes, int off, int len) throws IOException {
        sendSocket.send(ByteBuffer.wrap(bytes, off, len), socketAddress);
    }

    @Override
    public void send(ByteBuffer b) throws IOException {
        sendSocket.send(b, socketAddress);
    }

    public InetSocketAddress getAddress() {
        return socketAddress;
    }

    public NetworkInterface getInterface() {
        return iface;
    }

    public void join() throws IOException {
        if ( address == null ) {
            address = InetAddress.getByName(conf.getMcastAdr());
        }
        socketAddress = new InetSocketAddress(address,conf.getPort());
        if ( iface == null && conf.getIfacAdr() != null) {
            iface =NetworkInterface.getByInetAddress(Inet4Address.getByName(conf.getIfacAdr() ));
            if ( iface == null ) {
                iface = NetworkInterface.getByInetAddress( Inet4Address.getByName(conf.getIfacAdr() ));
            }
            if ( iface == null ) {
                FCLog.log("Could not find a network interface named '" + conf.getIfacAdr() + "'");
            }
        }
        receiveSocket = ceateSocket();
        sendSocket = ceateSocket();

        MembershipKey key = receiveSocket.join(address, iface);
        FCLog.log("Connecting to interface " + iface.getName()+ " on address " + address + " " + conf.getPort()+" dgramsize:"+getConf().getDgramsize());

    }

    private DatagramChannel ceateSocket() throws IOException {
        DatagramChannel channel = DatagramChannel.open(StandardProtocolFamily.INET)
                .setOption(StandardSocketOptions.SO_REUSEADDR, true)
                .setOption(StandardSocketOptions.IP_MULTICAST_IF, iface)
                .setOption(StandardSocketOptions.SO_RCVBUF, conf.getSocketReceiveBufferSize())
                .setOption(StandardSocketOptions.IP_TOS, conf.getTrafficClass())
                .setOption(StandardSocketOptions.IP_MULTICAST_LOOP, conf.isLoopBack())
                .setOption(StandardSocketOptions.IP_MULTICAST_TTL, conf.getTtl())
                .bind(new InetSocketAddress(conf.getPort()));
        channel.configureBlocking(false);
        return channel;
    }

    @Override
    public PhysicalTransportConf getConf() {
        return conf;
    }
}
