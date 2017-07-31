package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

/**
 * Created by ruedi on 19.06.17.
 *
 * refactoring of common stuff out of UndertowHttpServerConnector
 *
 */
public abstract class AbstractHttpServerConnector implements ActorServerConnector {

    public static int REQUEST_RESULTING_FUTURE_TIMEOUT = 3000; // max wait time for a returned promise to fulfil
    public static long SESSION_TIMEOUT_MS = TimeUnit.MINUTES.toMillis(30); // 30 minutes

    protected Actor facade;
    protected HashMap<String,HttpObjectSocket> sessions = new HashMap<>(); // use only from facade thread

    protected FSTConfiguration conf = FSTConfiguration.createJsonConfiguration(false,false); // used for authdata
    protected Function<ObjectSocket, ObjectSink> factory;
    protected long sessionTimeout = SESSION_TIMEOUT_MS;
    protected volatile boolean isClosed = false;
    protected ActorServer actorServer;
    protected Function<KHttpExchange,ConnectionAuthResult> connectionVerifier;

    public AbstractHttpServerConnector(Actor facade) {
        this.facade = facade;
        facade.delayed( HttpObjectSocket.LP_TIMEOUT/2, () -> houseKeeping() );
    }

    public void houseKeeping() {
//        System.out.println("----------------- HOUSEKEEPING ------------------------");
        long now = System.currentTimeMillis();
        ArrayList<String> toRemove = new ArrayList<>(0);
        sessions.entrySet().forEach( entry -> {
            HttpObjectSocket socket = entry.getValue();
            if ( now- socket.getLongPollTaskTime() >= HttpObjectSocket.LP_TIMEOUT/2 ) {
                socket.triggerLongPoll();
            }
            if ( now- socket.getLastUse() > getSessionTimeout() ) {
                toRemove.add(entry.getKey());
            }
        });
        toRemove.forEach(sessionId -> closeSession(sessionId));
        if ( ! isClosed ) {
            facade.delayed( HttpObjectSocket.LP_TIMEOUT/4, () -> houseKeeping() );
        }
    }

    public void setSessionTimeout(long sessionTimeout) {
        this.sessionTimeout = sessionTimeout;
    }

    public long getSessionTimeout() {
        return sessionTimeout;
    }

    protected HttpObjectSocket restoreSessionFromId(String sessionId) {
        if ( facade instanceof SessionResurrector)
        {
            ((SessionResurrector)facade.getActorRef()).restoreRemoteRefConnection(sessionId);
            HttpObjectSocket sock = new HttpObjectSocket( sessionId, () -> facade.execute( () -> closeSession(sessionId))) {
                @Override
                protected int getObjectMaxBatchSize() {
                    return HttpObjectSocket.HTTP_BATCH_SIZE;
                }

                @Override
                public String getConnectionIdentifier() {
                    return sessionId;
                }

            };
            sessions.put( sock.getSessionId(), sock );
            ObjectSink sink = factory.apply(sock);
            sock.setSink(sink);
            return sock;
        }
        return null;
    }

    public Function<KHttpExchange, ConnectionAuthResult> getConnectionVerifier() {
        return connectionVerifier;
    }

    public void setConnectionVerifier(Function<KHttpExchange, ConnectionAuthResult> connectionVerifier) {
        this.connectionVerifier = connectionVerifier;
    }

    protected void handleNewSession(KHttpExchange exchange ) {

        String sessionId = null;
        if ( connectionVerifier != null ) {
//            String token = exchange.getRequestHeaders().getFirst("token");
//            String uname = exchange.getRequestHeaders().getFirst("uname");
            ConnectionAuthResult denialReason = connectionVerifier.apply(exchange);
            if ( denialReason == null || denialReason.isError() ) {
                exchange.setResponseCode(403);
                exchange.send( denialReason != null ? denialReason.getError() : "expected ConnectionAuthResult, got null");
                exchange.endExchange();
                return;
            }
            sessionId = denialReason.getSid();
        } else
        {
            sessionId = UUID.randomUUID().toString();
        }
        String finalSessionId = sessionId;
        HttpObjectSocket sock = new HttpObjectSocket( sessionId, () -> facade.execute( () -> closeSession(finalSessionId))) {
            @Override
            protected int getObjectMaxBatchSize() {
                // huge batch size to make up for stupid sync http 1.1 protocol enforcing latency inclusion
                return HttpObjectSocket.HTTP_BATCH_SIZE;
            }

            @Override
            public String getConnectionIdentifier() {
                return sessionId;
            }

        };
        sessions.put( sock.getSessionId(), sock );
        ObjectSink sink = factory.apply(sock);
        sock.setSink(sink);

        // send auth response
        byte[] response = conf.asByteArray(sock.getSessionId());
        exchange.sendAuthResponse(response,sessionId);
    }

    protected HttpObjectSocket closeSession(String sessionId) {
        Log.Info(this,sessionId+" closed");
        HttpObjectSocket httpObjectSocket = sessions.get(sessionId);
        if ( httpObjectSocket != null ) {
            httpObjectSocket.sinkClosed();
        }
        return sessions.remove(sessionId);
    }

    @Override
    public void connect(Actor facade, Function<ObjectSocket, ObjectSink> factory) throws Exception {
        this.facade = facade;
        this.factory = factory;
    }

    @Override
    public IPromise closeServer() {
        isClosed = true;
        return new Promise<>(null); // FIXME: should wait for real finish
    }

    public void setActorServer(ActorServer actorServer) {
        this.actorServer = actorServer;
    }

    public ActorServer getActorServer() {
        return actorServer;
    }


}
