package org.nustaq.fastcast.config;

/**
 * Created by ruedi on 29.11.2014.
 */
public class FCPublisherConf {

    String transport = "default";
    int topicId;

    // Buffer sizes are very important. For high volume senders, the send buffer must be large
    // these defaults hold for moderate traffic

    ///////////////////////////////////////////////////////////////////////////////
    //
    // sender buffers
    //
    ///////////////////////////////////////////////////////////////////////////////

    // overall send history
    int numPacketHistory = 100_000*3;

    ///////////////////////////////////////////////////////////////////////////////
    //
    // sender timings
    //
    ///////////////////////////////////////////////////////////////////////////////

    // defines at which rate this topicId send messages.
    // This is the major rate limiting throttle influencing throughput and overload control
    // if too low => lots of retransmission
    // if too high => no throughput
    int sendPauseMicros = 300;

    // when a sender does not send on a topicId for this time, drop it and free memory.
    // if the sender starts sending again, a resync will be done as if the sender has
    // joined the cluster as a new node
    long heartbeatInterval = 200;    // sent per topicId, ms. detects senderTimeoutMillis

    long flowControlInterval = 1000; // time window(ms) flow control uses to determine send rate + stats reset rate

    public FCPublisherConf(String transport, int topicId ) {
        this.topicId = topicId;
        this.transport = transport;
    }

    ///////////////////////////////////////////////////////////////////////////////
    //
    // sender misc (threading, locking
    //
    ///////////////////////////////////////////////////////////////////////////////

    boolean optForLatency = true; // do not chain msg across packat if not necessary
    String flowControlClass = null;// StupidFlowControl.class.getName();  // set null to delegate flow control to service
    private int dGramRate; // ignored if 0

    public String getFlowControlClass() {
        return flowControlClass;
    }

    public void setFlowControlClass(String flowControlClass) {
        this.flowControlClass = flowControlClass;
    }

    public boolean isOptForLatency() {
        return optForLatency;
    }

    public void setOptForLatency(boolean optForLatency) {
        this.optForLatency = optForLatency;
    }

    public int getNumPacketHistory() {
        return numPacketHistory;
    }

    public void setNumPacketHistory(int numPacketHistory) {
        this.numPacketHistory = numPacketHistory;
    }

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }

    public int getTopicId() {
        return topicId;
    }

    public void setTopicId(int topicId) {
        this.topicId = topicId;
    }

    public int getSendPauseMicros() {
        return sendPauseMicros;
    }

//    public void setSendPauseMicros(int sendPauseMicros) {
//        if ( sendPauseMicros != -1 ) {
//            FCLog.get().warn("both sendpause and DGramRate configured. Expect only one of them to be set.");
//        }
//        this.sendPauseMicros = sendPauseMicros;
//    }

    public long getHeartbeatInterval() {
        return heartbeatInterval;
    }

    public void setHeartbeatInterval(long heartbeatInterval) {
        this.heartbeatInterval = heartbeatInterval;
    }

    public long getFlowControlInterval() {
        return flowControlInterval;
    }

    public void setFlowControlInterval(long flowControlInterval) {
        this.flowControlInterval = flowControlInterval;
    }

    /**
     * warning: this overwrite sendPauseMicros
     * @param DGramRate
     */
    public void setDGramRate(int DGramRate) {
        if ( DGramRate == 0 )
            return;
        int slowdown = 1000*1000/DGramRate;
        if ( slowdown < 1 ) {
            slowdown = 1;
        }
        sendPauseMicros = slowdown;
        this.dGramRate = DGramRate;
    }

    public int getDGramRate() {
        return dGramRate;
    }


}
