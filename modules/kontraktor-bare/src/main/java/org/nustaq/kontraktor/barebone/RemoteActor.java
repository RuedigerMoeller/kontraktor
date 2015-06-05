package org.nustaq.kontraktor.barebone;

/**
 * Created by ruedi on 05/06/15.
 */
public class RemoteActor {
    protected int remoteId;
    protected RemoteActorConnection con;
    String name;

    public RemoteActor(String name, int remoteId, RemoteActorConnection con) {
        this.remoteId = remoteId;
        this.con = con;
        this.name = name;
    }

    public void tell( String methodName, Object ... arguments ) {
        con.addRequest(new BBRemoteCallEntry(remoteId,0,methodName,arguments,0), null);
    }

    public BBPromise ask( String methodName, Object ... arguments ) {
        BBPromise res = new BBPromise<>();
        con.addRequest(new BBRemoteCallEntry(remoteId,0,methodName,arguments,0),res);
        return res;
    }

    public int getRemoteId() {
        return remoteId;
    }

    public RemoteActorConnection getCon() {
        return con;
    }

    @Override
    public String toString() {
        return "RemoteActor{" +
                   "remoteId=" + remoteId +
                   ", con=" + con +
                   ", name='" + name + '\'' +
                   '}';
    }
}
