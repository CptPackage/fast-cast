package de.ruedigermoeller.fastcast.transport;

import de.ruedigermoeller.fastcast.util.FCLog;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.nodes.Node;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

/**
 * Copyright (c) 2012, Ruediger Moeller. All rights reserved.
 * <p/>
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * <p/>
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 * <p/>
 * Date: 05.06.13
 * Time: 18:27
 * To change this template use File | Settings | File Templates.
 */
public class FCSocketConf {

    public static String MCAST_NIO_SOCKET = "MCAST_NIO_SOCKET";
    public static String MCAST_SOCKET = "MCAST_SOCKET";
    public static String MCAST_IPC = "MCAST_IPC";

    String name;

    int dgramsize = 8000;
    private String ifacAdr = "eth0";
    String mcastAdr = "229.9.9.9";
    int port = 45555;
    int trafficClass = 0x08;
    boolean loopBack = true;
    int ttl = 2;
    int receiveBufferSize = 30000000; // used as file size for shmem
    int sendBufferSize = 640000;
    String transportType = MCAST_NIO_SOCKET;
    String queueFile; // for shared mem, identifies file path of mmapped file for this transport

    public FCSocketConf() {
    }

    public FCSocketConf(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDgramsize() {
        return dgramsize;
    }

    public void setDgramsize(int dgramsize) {
        this.dgramsize = dgramsize;
    }

    public String getIfacAdr() {
        if ( ! Character.isDigit(ifacAdr.charAt(0)) ) {
            Enumeration<NetworkInterface> nets = null;
            try {
                nets = NetworkInterface.getNetworkInterfaces();
                for (NetworkInterface netint : Collections.list(nets)) {
                    if ( netint.getDisplayName().equalsIgnoreCase(ifacAdr) ) {
                        Enumeration<InetAddress> inetAddresses = netint.getInetAddresses();
                        if ( inetAddresses.hasMoreElements() ) {
                            ifacAdr = inetAddresses.nextElement().getHostAddress();
                            break;
                        } else {
                            FCLog.get().warn("specified interface " + ifacAdr + " does not have an IP assigned");
                        }
                    }
                }
            } catch (SocketException e) {
                FCLog.log(e);  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        return ifacAdr;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public void setIfacAdr(String ifacAdr) {
        this.ifacAdr = ifacAdr;
    }

    public String getMcastAdr() {
        return mcastAdr;
    }

    public void setMcastAdr(String mcastAdr) {
        this.mcastAdr = mcastAdr;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getTrafficClass() {
        return trafficClass;
    }

    public void setTrafficClass(int trafficClass) {
        this.trafficClass = trafficClass;
    }

    public boolean isLoopBack() {
        return loopBack;
    }

    public void setLoopBack(boolean loopBack) {
        this.loopBack = loopBack;
    }

    public int getTtl() {
        return ttl;
    }

    public void setTtl(int ttl) {
        this.ttl = ttl;
    }

    public int getReceiveBufferSize() {
        return receiveBufferSize;
    }

    public void setReceiveBufferSize(int receiveBufferSize) {
        this.receiveBufferSize = receiveBufferSize;
    }

    public int getSendBufferSize() {
        return sendBufferSize;
    }

    public void setSendBufferSize(int sendBufferSize) {
        this.sendBufferSize = sendBufferSize;
    }


    public static void write(String finam,FCSocketConf data) throws IOException {
        DumperOptions opt = new DumperOptions();
        opt.setPrettyFlow(true);
        Representer representer = new Representer();
//        representer.addClassTag(FCSocketConf.class, new Tag("!topic"));
//        representer.addClassTag(FCSocketConf.class, new Tag("!socket"));
        Yaml yaml = new Yaml(representer,opt);
        FileWriter wri = new FileWriter(finam);
        wri.write(yaml.dumpAsMap(data));
        wri.close();
//        System.out.println(yaml.dumpAsMap(data));
    }

    public static FCSocketConf read(InputStream in) throws IOException {
        Yaml yaml = new Yaml(new Constructor(){
            @Override
            protected Class<?> getClassForNode(Node node) {
                String name = node.getTag().getValue();
                if ( "!topic".equals(name)) {
                    return FCSocketConf.class;
                }
                if ( "!socket".equals(name) ) {
                    return FCSocketConf.class;
                }
                return super.getClassForNode(node);
            }
        });
        FCSocketConf conf = (FCSocketConf) yaml.loadAs(in, FCSocketConf.class);
        in.close();
        return conf;
    }

    public static FCSocketConf read(String finam) throws IOException {
        Yaml yaml = new Yaml(new Constructor(){
            @Override
            protected Class<?> getClassForNode(Node node) {
                String name = node.getTag().getValue();
                if ( "!topic".equals(name)) {
                    return FCSocketConf.class;
                }
                if ( "!socket".equals(name) ) {
                    return FCSocketConf.class;
                }
                return super.getClassForNode(node);
            }
        });
        FileReader reader = new FileReader(finam);
        FCSocketConf conf = (FCSocketConf) yaml.loadAs(reader, FCSocketConf.class);
        reader.close();
        return conf;
    }

    public static void main( String arg[] ) throws IOException {
        FCSocketConf conf = new FCSocketConf();
        write("/tmp/fcconf.yaml",conf);
        FCSocketConf read = read("/tmp/fcconf.yaml");
        System.out.println("pok "+read);
    }

    public String getQueueFile() {
        return queueFile;
    }

    public void setQueueFile(String queueFile) {
        this.queueFile = queueFile;
    }
}
