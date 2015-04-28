package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServerAdapter;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

/**
 * Created by ruedi on 30.03.2015.
 *
 * multiplexes incoming packets/messages to a set of connections/session and manages/maps remote actor references
 *
 */
public abstract class ActorServerAdapter {

    protected List<ActorServerConnection> connections = new ArrayList<>();
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

    public List<ActorServerConnection> getConnections() {

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
            ActorServerConnection actorServerConnection = connections.get(i);
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

    /**
     * instantiated per client. Hold mapping of remote refs
     */
    public class ActorServerConnection extends RemoteRefRegistry {
        AtomicReference<ObjectSocket> objSocket = new AtomicReference<>(null);
        Actor facade;
        volatile boolean isObsolete = false;

        public ActorServerConnection() {
        }

        public ActorServerConnection(Coding coding) {
            super(coding);
        }

        @Override
        protected void configureConfiguration(Coding code) {
            super.configureConfiguration(code);
//                Register toReg = facade.getActor().getClass().getAnnotation(Register.class);
//                if (toReg!=null) {
//                    conf.registerClass(toReg.value());
//                }
        }

        public ActorServerConnection(ObjectSocket s, Actor facade) throws Exception {
            super();
            init(s, facade);
        }

        public void init(ObjectSocket s, Actor facade) {
            this.objSocket.set(s);
            this.facade = facade;
            this.disconnectHandler = closeListener;
            publishActor(facade); // so facade is always 1
        }

        public void start(boolean spawnThread) {
            if ( spawnThread ) { // if networking comes from a ws or http server, no own receivethread is required
                scheduleReceiveLoop(() -> {
                    try {
                        receiveLoop(objSocket.get());
                    } catch (Exception ex) {
                        Log.Warn(this, ex, "");
                    }
                    handleConnectionTermination();
                });
            }
            sendThread.scheduleSendLoop( this ).then(() -> handleConnectionTermination());
        }

        // cleanup after error/disconnect in case.
        public void handleConnectionTermination() {
            if (!virtualConnection) {
                terminateAndCleanUp();
            }
        }

        public void terminateAndCleanUp() {
            cleanUp(); // might be second time depending on thread model
            setTerminated(true);
            connections.remove(ActorServerConnection.this);
        }

        @Override
        public void close() {
            super.close();
            try {
                objSocket.get().close();
            } catch (IOException e) {
                Log.Warn(this,e,"");
            }
        }

        @Override
        public Actor getFacadeProxy() {
            return facade;
        }

        @Override
        public AtomicReference<ObjectSocket> getObjectSocket() {
            return objSocket;
        }

        /**
         * move my object socket to the given registry
         * @param prevRegistry
         */
        public void handOverTo(ActorServerConnection prevRegistry) {
            // ouch .. this can be done safely because a client is allowed to send the
            // next message only after having received a successful reconnect response ..
            WebSocketActorServerAdapter.MyWSObjectSocket newOS = (WebSocketActorServerAdapter.MyWSObjectSocket) objSocket.get();

            newOS.mergePendingWritesAndReplaceInRef(prevRegistry);
            getObjectSocket().set(null);
            setTerminated(true);
            setIsObsolete(true);
        }

        public boolean isObsolete() {
            return isObsolete;
        }

        /**
         * give the application a way to explecitely flag a connection as obsolete
         *
         */
        public void setIsObsolete(boolean isObsolete) {
            this.isObsolete = isObsolete;
        }
    }

    protected void scheduleReceiveLoop(Runnable runnable) {
        new Thread( runnable, "ActorConnnection.receiver").start();
    }


    static class ScheduleEntry {
        public ScheduleEntry( RemoteRefRegistry reg, Promise promise) {
            this.reg = reg;
            this.promise = promise;
        }

        RemoteRefRegistry reg;
        IPromise promise;
    }

    // currently a single thread per process is assigned to encoding (depends on object socket impl. WebObjectSocket impl just queues).
    // could be a bottleneck
    SenderLoop sendThread = new SenderLoop();

    /**
     * polls queues of remote actor proxies and serializes messages to their associated object sockets.
     *
     * Terminated / Disconnected remote actors (registries) are removed from the entry list,
     * so regular actor messages sent to a terminated remote actor queue up in its mailbox.
     * Callbacks/Future results from exported callbacks/futures still reach the object socket
     * as these are redirected directly inside serializers. Those queue up in the webobjectsocket's list,
     * as flush is not called anymore because of removement from SendLoop list.
     *
     * in short: regular messages to disconected remote actors queue up in mailbox, callbacks in object socket buffer
     *
     */
    class SenderLoop implements Runnable {

        ArrayList<ScheduleEntry> sendJobs = new ArrayList<>();
        BackOffStrategy backOffStrategy = new BackOffStrategy().setNanosToPark(1000*1000*2);

        public SenderLoop() {
            new Thread(this, "ActorServer.senderloop").start();
        }

        public IPromise scheduleSendLoop(RemoteRefRegistry reg) {
            synchronized (sendJobs) {
                Promise promise = new Promise();
                sendJobs.add(new ScheduleEntry(reg, promise));
                return promise;
            }
        }

        @Override
        public void run() {
            int count = 0;
            while( true ) {
                try {
                synchronized (sendJobs) {
                    for (int i = 0; i < sendJobs.size(); i++) {
                        ScheduleEntry entry = sendJobs.get(i);
                        if ( entry.reg.isTerminated() ) {
                            terminateEntry(i, entry, "terminated", null );
                            i--;
                            continue;
                        }
                        try {
                            if (entry.reg.singleSendLoop(entry.reg.getObjectSocket().get())) {
                                count = 0;
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            terminateEntry(i, entry, null, e);
                            i--;
                        }
                    }
                }
                backOffStrategy.yield(count++);
                } catch (Throwable t) {
                    Log.Warn(this,t);
                }
            }
        }

        protected void terminateEntry(int i, ScheduleEntry entry, Object res, Exception e) {
            entry.reg.stopRemoteRefs();
            sendJobs.remove(i);
            entry.promise.complete(res,e);
        }

    }

}
