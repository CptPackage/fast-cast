package org.nustaq.fastcast.remoting;

import org.nustaq.offheap.bytez.ByteSource;

/**
 * Created by ruedi on 29.11.2014.
 */
public interface FCPublisher {

    public boolean offer(ByteSource msg, int start, int len);
    public int getTopicId();

}
