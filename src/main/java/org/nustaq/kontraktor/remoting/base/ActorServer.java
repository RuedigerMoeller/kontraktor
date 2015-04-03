package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by ruedi on 30.03.2015.
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
     * @return wether to spwan a thread per client
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
                new Thread(() -> {
                    try {
                        currentObjectSocket.set(objSocket);
                        receiveLoop(objSocket);
                    } catch (Exception ex) {
                        Log.Warn(this, ex, "");
                    }
                    handleTermination();
                }, "receiver").start();
            }
            // FIXME: polling remoted actor proxies per client can be done with one single thread
            // currently one per client used
            new Thread(() -> {
                try {
                    currentObjectSocket.set(objSocket);
                    sendLoop(objSocket);
                } catch (Exception ex) {
                    Log.Warn(this,ex,"");
                }
                handleTermination();
            }, "sender").start();
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

}
