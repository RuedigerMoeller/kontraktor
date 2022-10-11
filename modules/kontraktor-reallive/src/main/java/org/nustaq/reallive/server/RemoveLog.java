package org.nustaq.reallive.server;

import org.nustaq.kontraktor.Callback;

import java.io.Serializable;

public interface RemoveLog {

    public static class RemoveLogEntry implements Serializable {
        long time;
        String key;

        public RemoveLogEntry(long time, String key) {
            this.time = time;
            this.key = key;
        }

        public long getTime() {
            return time;
        }

        public String getKey() {
            return key;
        }

        @Override
        public String toString() {
            return "RemoveLogEntry{" +
                "time=" + time +
                ", key='" + key + '\'' +
                '}';
        }
    }

    void add( long timeStamp, String recordKey );
    void query(long from, long to, Callback<RemoveLogEntry> en );

    void prune(long maxAge);
}
