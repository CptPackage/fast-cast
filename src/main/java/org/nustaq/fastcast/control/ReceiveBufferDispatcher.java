package org.nustaq.fastcast.control;

import org.nustaq.fastcast.packeting.PacketReceiveBuffer;
import org.nustaq.fastcast.packeting.TopicEntry;
import org.nustaq.fastcast.remoting.FCSubscriber;
import org.nustaq.fastcast.util.FCUtils;
import org.nustaq.offheap.structs.structtypes.StructString;

import java.util.HashMap;
import java.util.concurrent.Executor;

/**
 * Created with IntelliJ IDEA.
 * User: ruedi
 * Date: 14.08.13
 * Time: 00:42
 * To change this template use File | Settings | File Templates.
 */
public class ReceiveBufferDispatcher {

    Executor topicWideDeliveryThread;
    HashMap<StructString,PacketReceiveBuffer> bufferMap = new HashMap<StructString, PacketReceiveBuffer>();

    int packetSize;
    String clusterName;
    String nodeId;
    int historySize;
    int topic;
    FCSubscriber receiver;
    TopicEntry topicEntry;

    public ReceiveBufferDispatcher(int packetSize, String clusterName, String nodeId, TopicEntry entry, FCSubscriber rec) {
        receiver = rec;
        this.packetSize = packetSize;
        this.clusterName = clusterName;
        this.nodeId = nodeId;
        this.historySize = entry.getReceiverConf().getReceiveBufferPackets();
        this.topic = entry.getTopicId();
        topicEntry = entry;
        topicWideDeliveryThread = FCUtils.createIncomingMessageThreadExecutor("global delivery " + topicEntry.getTopicId(), topicEntry.getReceiverConf().getDecodeQSize() );
    }

    public TopicEntry getTopicEntry() {
        return topicEntry;
    }

    public PacketReceiveBuffer getBuffer(StructString sender) {
        PacketReceiveBuffer receiveBuffer = bufferMap.get(sender);
        if ( receiveBuffer == null ) {
            receiveBuffer = new PacketReceiveBuffer(packetSize,clusterName,nodeId,historySize,sender.toString(), topicEntry, receiver, topicWideDeliveryThread);
            bufferMap.put((StructString) sender.createCopy(),receiveBuffer);
        }
        return receiveBuffer;
    }

    /**
     * if a sender stops sending, remove from map to free memory
     * @param senderName
     */
    public void cleanup(String senderName) {
        StructString struct = new StructString(senderName);
        PacketReceiveBuffer packetReceiveBuffer = bufferMap.get(struct);
        bufferMap.remove(struct);
        packetReceiveBuffer.terminate();
//        FCRemotingListener remotingListener = FastCast.getRemoting().getRemotingListener();
//        if ( remotingListener != null )
//            remotingListener.senderDied(topicEntry.getTopicId(), topicEntry.getName(), senderName);
    }
}
