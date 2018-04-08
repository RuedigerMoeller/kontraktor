package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;

import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * wraps a connectableActor and automatically tries to reconnect in case it is down
 *
 * @param <T>
 */
public class ReconnectableRemoteRef<T extends Actor> {

    public static interface ReconnectableListener {
        void remoteDisconnected(Actor disconnected);
        void remoteConnected(Actor connected);
        default void tryConnect(ConnectableActor ca) {}
    }
    public static long RETRY_INTERVAL = 1000;

    public static ReconnectableListener loggingListener = new ReconnectableListener() {
        @Override
        public void remoteDisconnected(Actor disconnected) {
            Log.Info(ReconnectableRemoteRef.class,"remote disconnected "+disconnected);
        }

        @Override
        public void tryConnect(ConnectableActor ca) {
            Log.Info(ReconnectableRemoteRef.class,"try connect "+ca);
        }

        @Override
        public void remoteConnected(Actor connected) {
            Log.Info(ReconnectableRemoteRef.class,"remote connected "+ connected);
        }
    };

    Timer timer = new Timer();
    Supplier<ConnectableActor> connectableSup;
    T current;
    ReconnectableListener conListener;
    boolean terminate = false;
    int connectsUnderway = 0;

    public ReconnectableRemoteRef(ConnectableActor remoteActor, ReconnectableListener listener) {
        this( () -> remoteActor,listener);
    }

    public ReconnectableRemoteRef(Supplier<ConnectableActor> remoteActorSup, ReconnectableListener listener) {
        this.connectableSup = remoteActorSup;
        this.conListener = listener;
        connectWithRetry();
    }

    IPromise<T> connect() {
        if ( isOnline() ) {
            return new Promise<>(current);
        }
        Promise p = new Promise();
        ConnectableActor connectableActor = connectableSup.get();
        if (conListener != null) {
            conListener.tryConnect(connectableActor);
        }
        connectableActor.connect(null, disc -> {
            handleDisconnect();
            if (conListener != null)
                conListener.remoteDisconnected(disc);
        }).then((r, e) -> {
            if (r != null) {
                current = (T) r;
                if (conListener != null)
                    conListener.remoteConnected(current);
            }
            p.complete(r, e);
        });
        return p;
    }

    protected synchronized void handleDisconnect() {
        if ( current == null ) // double message as each connection trial creates a disconnect callback
            return;
        Log.Warn(this,"registry disconnected");
        if ( current != null ) {
            current.close();
            current = null;
        }
        connectWithRetry();
    }

    private void connectWithRetry() {
        // immediately try reconnection
        connect().then( (r,e) -> {
           if ( ! isOnline() && ! terminate ) {
               timer.schedule(
                   new TimerTask() {
                       @Override public void run() {
                           connectWithRetry();
                       }
                   },
                   RETRY_INTERVAL
               );
           }
        });
    }

    public boolean isOnline() {
        return current != null;
    }

    public T get() {
        return current;
    }

    public void terminate() {
        this.terminate = true;
    }
}
