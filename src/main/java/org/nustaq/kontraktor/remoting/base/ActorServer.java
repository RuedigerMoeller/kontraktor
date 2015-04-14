package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.spa.FourKSession;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Created by ruedi on 30.03.2015.
 *
 * multiplexes incoming packets/messages to a set of connections/session and manages/maps remote actor references
 *
 */
public abstract class ActorServer {


    protected List<ActorServerConnection> connections = new ArrayList<>();

    protected Consumer<Actor> closeListener;
    protected Actor facade;
    protected volatile boolean terminated = false;

    public ActorServer(Actor proxy) {
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
        clientConnection.start(isThreadPerClient());
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

    public IPromise closeConnection() {
        setTerminated(true);
        closeSocket();
        return new Promise<>("");
    }

    public class ActorServerConnection extends RemoteRefRegistry {
        ObjectSocket objSocket;
        Actor facade;

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
            this.objSocket = s;
            this.facade = facade;
            this.disconnectHandler = closeListener;
            publishActor(facade); // so facade is always 1
        }

        public void start(boolean spawnThread) {
            if ( spawnThread ) {
                scheduleReceiveLoop(() -> {
                    try {
                        currentObjectSocket.set(objSocket);
                        receiveLoop(objSocket);
                    } catch (Exception ex) {
                        Log.Warn(this, ex, "");
                    }
                    handleTermination();
                });
            }
            // FIXME: polling remoted actor proxies per client can be done with one single thread
            // currently one per client used
            sendThread.scheduleSendLoop( objSocket, this ).then(() -> handleTermination());
        }

        // cleanup after error/disconnect.
        public void handleTermination() {
            cleanUp(); // might be second time depending on thread model
            setTerminated(true);
            connections.remove(ActorServerConnection.this);
        }

        @Override
        public void close() {
            super.close();
            try {
                objSocket.close();
            } catch (IOException e) {
                Log.Warn(this,e,"");
            }
        }

        @Override
        public Actor getFacadeProxy() {
            return facade;
        }

        public ObjectSocket getObjSocket() {
            return objSocket;
        }
    }

    protected void scheduleReceiveLoop(Runnable runnable) {
        new Thread( runnable, "ActorConnnection.receiver").start();
    }


    static class ScheduleEntry {
        public ScheduleEntry(ObjectSocket channel, RemoteRefRegistry reg, Promise promise) {
            this.channel = channel;
            this.reg = reg;
            this.promise = promise;
        }

        ObjectSocket channel;
        RemoteRefRegistry reg;
        IPromise promise;
    }

    // currently a single thread per process is assigned to encoding (depends on object socket impl. WebObjectSocket impl just queues).
    // could be a bottleneck
    public static SenderLoop sendThread = new SenderLoop();
    static class SenderLoop implements Runnable {

        ArrayList<ScheduleEntry> sendJobs = new ArrayList<>();
        BackOffStrategy backOffStrategy = new BackOffStrategy();

        public SenderLoop() {
            new Thread(this, "ActorServer.senderloop").start();
        }

        public IPromise scheduleSendLoop(ObjectSocket socket, RemoteRefRegistry reg) {
            synchronized (sendJobs) {
                Promise promise = new Promise();
                sendJobs.add(new ScheduleEntry(socket, reg, promise));
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
                            if (entry.reg.singleSendLoop(entry.channel)) {
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
