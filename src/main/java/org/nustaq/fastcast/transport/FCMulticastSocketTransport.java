package org.nustaq.fastcast.transport;

import org.nustaq.fastcast.config.FCSocketConf;
import org.nustaq.fastcast.util.FCLog;

import java.io.*;
import java.net.*;

/**
 * Created with IntelliJ IDEA.
 * User: moelrue
 * Date: 06.06.13
 * Time: 12:32
 * To change this template use File | Settings | File Templates.
 */
public class FCMulticastSocketTransport implements Transport {

    FCSocketConf conf;
    MulticastSocket socket;
    NetworkInterface iface;
    InetSocketAddress address;

    public FCMulticastSocketTransport(FCSocketConf conf) {
        System.setProperty("java.net.preferIPv4Stack","true" );
        this.conf = conf;
    }

    public void join() throws IOException {
        if ( address == null ) {
            address = new InetSocketAddress(InetAddress.getByName(conf.getMcastAdr()),conf.getPort());
        }
        if ( iface == null && conf.getIfacAdr()  != null) {
            iface = NetworkInterface.getByName(conf.getIfacAdr() );
            if ( iface == null ) {
                iface = NetworkInterface.getByInetAddress( Inet4Address.getByName(conf.getIfacAdr() ));
            }
            if ( iface == null ) {
                FCLog.log("Could not find a network interface named '" + conf.getIfacAdr() + "'");
            }
        }
        socket = new MulticastSocket(conf.getPort());
        if ( iface != null ) {
            socket.setNetworkInterface(iface);
        }
        socket.setReceiveBufferSize(conf.getReceiveBufferSize());
        socket.setSendBufferSize(conf.getSendBufferSize());
        socket.setTrafficClass(conf.getTrafficClass());
        socket.setLoopbackMode(!conf.isLoopBack());
        socket.setTimeToLive(conf.getTtl());
        socket.joinGroup(InetAddress.getByName(conf.getMcastAdr()));

        FCLog.log("Connecting to interface " + conf.getIfacAdr() + " on address " + conf.getMcastAdr() + " " + conf.getPort());
    }

    @Override
    public FCSocketConf getConf() {
        return conf;
    }

    public boolean receive(DatagramPacket pack) throws IOException {
        socket.receive(pack);
        return true;
    }

    public void send(DatagramPacket pack) throws IOException {
        pack.setSocketAddress(address);
        socket.send(pack);
    }

    public InetSocketAddress getAddress() {
        return address;  //To change body of implemented methods use File | Settings | File Templates.
    }

    public NetworkInterface getInterface() {
        return iface;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
