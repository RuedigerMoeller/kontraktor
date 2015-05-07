package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.remoting.WriteObjectSocket;
import org.nustaq.kontraktor.remoting.websocket.WebSocketActorServerAdapter;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * instantiated per client. Hold mapping of remote refs
 */
public class ActorServerConnection extends RemoteRefRegistry {
    private ActorServerAdapter actorServerAdapter;
    AtomicReference<ObjectSocket> objSocket = new AtomicReference<>(null);
    Actor facade;
    volatile boolean isObsolete = false;

    public ActorServerConnection(ActorServerAdapter actorServerAdapter) {
        this.actorServerAdapter = actorServerAdapter;
    }

    public ActorServerConnection(ActorServerAdapter actorServerAdapter, Coding coding) {
        super(coding);
        this.actorServerAdapter = actorServerAdapter;
    }

    public ActorServerConnection(ActorServerAdapter actorServerAdapter, ObjectSocket s, Actor facade) throws Exception {
        super();
        this.actorServerAdapter = actorServerAdapter;
        init(s, facade);
    }

    public void init(ObjectSocket s, Actor facade) {
        this.objSocket.set(s);
        this.facade = facade;
        this.disconnectHandler = actorServerAdapter.closeListener;
        publishActor(facade); // so facade is always 1
    }

    public void start(boolean spawnThread) {
        if ( spawnThread ) { // if networking comes from a ws or http server, no own receivethread is required
            actorServerAdapter.scheduleReceiveLoop(() -> {
                try {
                    receiveLoop(objSocket.get());
                } catch (Exception ex) {
                    Log.Warn(this, ex, "");
                }
                handleConnectionTermination();
            });
        }
        actorServerAdapter.sendThread.scheduleSendLoop( this ).then(() -> handleConnectionTermination());
    }

    // cleanup after error/disconnect in case.
    public void handleConnectionTermination() {
        if (!actorServerAdapter.virtualConnection) {
            terminateAndCleanUp();
        }
    }

    public void terminateAndCleanUp() {
        cleanUp(); // might be second time depending on thread model
        setTerminated(true);
        actorServerAdapter.connections.remove(ActorServerConnection.this);
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

}
