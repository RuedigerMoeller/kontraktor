package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.RemoteRegistry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by ruedi on 30.03.2015.
 *
 * multiplexes incoming packets/messages to a set of connections/session and manages/maps remote actor references
 *
 */
public abstract class ActorServerAdapter {

    protected List<RemoteRegistry> connections = new ArrayList<>();
    protected Consumer<Actor> closeListener;
    protected Actor facade;
    protected volatile boolean terminated = false;
    /**
     * if virtual connection == false, an actor is automatically unpublished
     * once the underlying network connection closes, so all remoteActor references
     * and remote callbacks/promises are invalidated.
     *
     * if virtual connection == true, a connection close does not terminate the remote
     * connection, so an implementation may try to reconnect or use a fallback communication mechanism.
     * Messages sent meanwhile have to get buffered somehow by the underyling object socket implementation.
     */
    protected boolean virtualConnection = false;

    public ActorServerAdapter(Actor proxy) {
        this.facade = proxy;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public Actor getFacade() {
        return facade;
    }

    public List<RemoteRegistry> getConnections() {

        return connections;
    }

    /**
     * invalidate the full adapter, closes all connections
     *
     * @param terminated
     */
    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
        connections.forEach( (con) -> con.setTerminated(true) );
    }

    /**
     * consumes calling thread !!
     * override empty for event driven connections
     *
     * @throws IOException
     *
     * returned future serves
     */
    public void acceptLoop() throws Exception {
        try {
            connectServerSocket();
            while (!terminated) {
                ActorServerConnection clientConnection = acceptConnections();
                doAccept(clientConnection);
            }
        } finally {
            setTerminated(true);
        }
    }

    public void doAccept(ActorServerConnection clientConnection) {
        connections.add(clientConnection);
        cleanUpConnections();
        System.out.println("ActorServerAdapter number of connections: "+connections.size());
        clientConnection.start(isThreadPerClient());
    }

    long lastCleanup = System.currentTimeMillis();
    private void cleanUpConnections() {
        if ( System.currentTimeMillis() - lastCleanup < 3000 )
            return;
        lastCleanup = System.currentTimeMillis();
        for (int i = 0; i < connections.size(); i++) {
            RemoteRegistry actorServerConnection = connections.get(i);
            if ( actorServerConnection.isObsolete() || actorServerConnection.isTerminated() ) {
                actorServerConnection.close();
                connections.remove(i);
                i--;
            }
        }
    }

    /**
     * @return wether to spawn a thread per client
     */
    protected boolean isThreadPerClient() {
        return true;
    }

    // return new connection instance. may block/return null
    protected abstract ActorServerConnection acceptConnections() throws Exception;
//    {
//        Socket connectionSocket = welcomeSocket.accept();
//        return new ActorServerConnection(connectionSocket, facade);
//    }

    // do initial netwrk connect
    protected abstract void connectServerSocket() throws Exception;
//    {
//        welcomeSocket = new ServerSocket(port);
//        Log.Info(this, facade.getActor().getClass().getName() + " running on " + welcomeSocket.getLocalPort());
//    }

    // cleanup network connection
    protected abstract void closeSocket();
//    {
//        try {
//            welcomeSocket.close();
//        } catch (IOException e) {
//            Log.Warn(this, e + "");
//        }
//    }

    /**
     * terminates everything (all subconnection)
     * @return
     */
    public IPromise closeConnection() {
        setTerminated(true);
        closeSocket();
        return new Promise<>("");
    }

    protected void scheduleReceiveLoop(Runnable runnable) {
        new Thread( runnable, "ActorConnnection.receiver").start();
    }


    static class ScheduleEntry {
        public ScheduleEntry( RemoteRegistry reg, Promise promise) {
            this.reg = reg;
            this.promise = promise;
        }

        RemoteRegistry reg;
        IPromise promise;
    }

    // currently a single thread per process is assigned to encoding (depends on object socket impl. WebObjectSocket impl just queues).
    // could be a bottleneck
    SenderLoop sendThread = new SenderLoop();

}
