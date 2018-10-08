package org.nustaq.kontraktor.remoting.encoding;

public class TrackedRemoteCallEntry extends RemoteCallEntry {

    int trackingId;

    public TrackedRemoteCallEntry() {
    }

    public TrackedRemoteCallEntry(long futureKey, long receiverKey, String method, Object[] args, byte[] serializedArgs) {
        super(futureKey, receiverKey, method, args, serializedArgs);
    }

    @Override
    public int getTrackingId() {
        return trackingId;
    }

    public void _internal_setTrackingId(int id) {
        trackingId = id;
    }
}
