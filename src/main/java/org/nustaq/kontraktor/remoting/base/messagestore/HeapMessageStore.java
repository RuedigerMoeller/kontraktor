package org.nustaq.kontraktor.remoting.base.messagestore;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by ruedi on 13/04/15.
 *
 * writes/reads must be single threaded per queue
 *
 */
public class HeapMessageStore implements MessageStore {

    int maxStoreLength = 64;
    ConcurrentHashMap<CharSequence,StoreEntry> map = new ConcurrentHashMap<>();

    public HeapMessageStore(int maxStoreLength) {
        this.maxStoreLength = maxStoreLength;
    }

    @Override
    public Object getMessage(CharSequence queueId, long sequence) {
        StoreEntry byteSources = map.get(queueId);
        if ( byteSources != null ) {
            return byteSources.get(sequence);
        }
        return null;
    }

    @Override
    public void putMessage(CharSequence queueId, long sequence, Object message) {
        StoreEntry byteSources = map.get(queueId);
        if ( byteSources == null ) {
            byteSources = new StoreEntry(maxStoreLength);
            map.put(queueId,byteSources);
        }
        byteSources.add(message,sequence);
    }

    @Override
    public void confirmMessage(CharSequence queueId, long sequence) {
        StoreEntry byteSources = map.get(queueId);
        if ( byteSources != null ) {
            byteSources.confirm(sequence);
        }
    }

    @Override
    public void killQueue(CharSequence queueId) {
        map.remove(queueId);
    }

    static class StoreEntry {

        public StoreEntry(int len) {
            messages = new Object[len];
            sequences = new long[len];
        }

        Object[] messages;
        long sequences[];
        int writePos;
        int readPos;

        public void add(Object msg, long seq) {
            messages[writePos] = msg;
            sequences[writePos] = seq;
            writePos++;
            if ( writePos == messages.length ) {
                writePos = 0;
            }
            if ( writePos == readPos ) {
                readPos++;
                if ( readPos == messages.length ) {
                    readPos = 0;
                }
            }
        }

        public void confirm(long seq) {
            int idx = readPos;
            for (int i = 0; i < messages.length; i++) {
                if ( seq <= sequences[idx] ) {
                    messages[idx] = null;
                    sequences[idx] = 0;
                } else {
                    readPos = idx;
                    if ( readPos < 0 )
                        readPos = 0;
                    return;
                }
                idx++;
                if ( idx == messages.length )
                    idx = 0;
            }
            readPos = idx-1;
            if ( readPos < 0 )
                readPos = 0;
        }

        public Object get(long seq) {
            int idx = readPos;
            for (int i = 0; i < messages.length; i++) {
                if ( seq == sequences[idx] )
                    return messages[idx];
                idx++;
                if ( idx == messages.length )
                    idx = 0;
            }
            return null;
        }

    }
}
