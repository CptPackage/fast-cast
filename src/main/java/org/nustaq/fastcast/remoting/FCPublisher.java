package org.nustaq.fastcast.remoting;

import org.nustaq.offheap.bytez.ByteSource;

/**
 * Created by ruedi on 29.11.2014.
 */
public interface FCPublisher {

    public boolean offer(ByteSource msg, long start, int len, boolean doFlush);
    public int getTopicId();

}
