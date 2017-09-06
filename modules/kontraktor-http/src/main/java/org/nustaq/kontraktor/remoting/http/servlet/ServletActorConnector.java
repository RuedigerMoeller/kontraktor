package org.nustaq.kontraktor.remoting.http.servlet;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.AbstractHttpServerConnector;
import org.nustaq.kontraktor.remoting.http.HttpObjectSocket;
import org.nustaq.kontraktor.remoting.http.KHttpExchange;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Created by ruedi on 19.06.17.
 */
public class ServletActorConnector extends AbstractHttpServerConnector {
    KontraktorServlet servlet;

    public ServletActorConnector(Actor facade, KontraktorServlet servlet, Coding coding, Consumer<Actor> disconnectCallback) {
        super(facade);
        this.servlet = servlet;
        try {
            setConnectionVerifier(connectionVerifier);
            setSessionTimeout(sessionTimeout);
            actorServer = new ActorServer( this, facade, coding == null ? new Coding(SerializerType.FSTSer) : coding );
            setActorServer(actorServer);
            actorServer.start(disconnectCallback);
        } catch (Exception e) {
            Log.Error(null, e);
        }
        try {
            actorServer.start( fail -> System.out.println("FIXME discon:"+fail));
        } catch (Exception e) {
            Log.Error(this,e);
        }
    }

    protected void requestReceived(String endpointPrefix, AsyncContext aCtx, byte[] postData) {
//        try {
//            System.out.println("REQ:"+new String(postData,"UTF-8")+"<");
//            System.out.println(Thread.currentThread().getName());
//            System.out.println();
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
        String path = ((HttpServletRequest)aCtx.getRequest()).getPathInfo();
        if ( path == null )
            path = "";
        if ( endpointPrefix != null ) // assume has been matched prior
            path = path.substring(endpointPrefix.length());
        // already executed in facade thread
        while ( path.startsWith("/") )
            path = path.substring(1);
        if ( path.trim().length() > 0 ) {
            String[] split = path.split("/");
            HttpObjectSocket httpObjectSocket = sessions.get(split[0]);
            if ( httpObjectSocket != null ) {
                handleClientRequest(aCtx, httpObjectSocket, postData, split.length > 1 ? split[1] : null);
            } else {
                httpObjectSocket = restoreSessionFromId(split[0]);
                if ( httpObjectSocket != null ) {
                    // path was not a session id => process direct post request
//                    handleClientRequest(exchange, httpObjectSocket, postData, split.length > 1 ? split[1] : null);
                } else {
                    HttpServletResponse response = (HttpServletResponse) aCtx.getResponse();
                    response.setStatus(401);
                    aCtx.complete();
                }
            }
        } else { // new session
            Object auth = null;
            // create connection. postdata is auth data
            if ( postData != null && postData.length > 0 ) {
                auth = conf.asObject(postData);
            }

            // auth check goes here
            handleNewSession(new ServletKHttpExchangeImpl(servlet,aCtx));
        }
    }

    public void handleClientRequest(AsyncContext aCtx, HttpObjectSocket httpObjectSocket, byte[] postData, String lastSeenSequence) {

        // executed in facade thread
        httpObjectSocket.updateTimeStamp(); // keep alive
        Object received[] = (Object[]) httpObjectSocket.getConf().asObject(postData);

        boolean isEmptyLP = received.length == 1 && received[0] instanceof Number;

        if ( ! isEmptyLP ) {
            handleRegularRequest(aCtx, httpObjectSocket, received);
            return;
        }

        // read next batch of pending messages from binary queue and send them
        Pair<Runnable,KHttpExchange> lpTask = createLongPollTask( new ServletKHttpExchangeImpl(servlet,aCtx), httpObjectSocket );

        // release previous long poll request if present
        httpObjectSocket.cancelLongPoll();
        httpObjectSocket.setLongPollTask(lpTask);
    }

    protected void handleRegularRequest(AsyncContext aCtx, HttpObjectSocket httpObjectSocket, Object[] received) {
        ArrayList<IPromise> futures = new ArrayList<>();
        httpObjectSocket.getSink().receiveObject(received, futures, ((HttpServletRequest) aCtx.getRequest()).getHeader("JWT") );

        Runnable reply = () -> {
            // piggy back outstanding lp messages, outstanding lp request is untouched
            Pair<byte[], Integer> nextQueuedMessage = httpObjectSocket.getNextQueuedMessage();
            byte response[] = nextQueuedMessage.getFirst();
            aCtx.getResponse().setContentLength(response.length);
            if (response.length == 0) {
                aCtx.complete();
            } else {
                httpObjectSocket.storeLPMessage(nextQueuedMessage.cdr(), response);
                try {
                    ((HttpServletResponse) aCtx.getResponse()).setStatus(200);
                    aCtx.getResponse().setCharacterEncoding("UTF-8");
                    aCtx.getResponse().setContentType("text/html; charset=utf-8");
                    String respString = new String(response, "UTF-8");
                    System.out.println("send resp "+respString);
                    aCtx.getResponse().getWriter().write(respString); // FIXME: ASYNC
                    aCtx.getResponse().getWriter().close();
                } catch (IOException e) {
                    Log.Error(this,e);
                }
                aCtx.complete();
            }
        };
        if ( futures == null || futures.size() == 0 ) {
            reply.run();
        } else {
            Actors.all((List) futures).timeoutIn(REQUEST_RESULTING_FUTURE_TIMEOUT).then( () -> {
                reply.run();
            }).onTimeout( () -> reply.run() );
        }
    }

    protected Pair<Runnable,KHttpExchange> createLongPollTask(KHttpExchange exchange, HttpObjectSocket httpObjectSocket ) {
        return new Pair<>(
            () -> {
                Pair<byte[], Integer> nextQueuedMessage = httpObjectSocket.getNextQueuedMessage();
                byte response[] = nextQueuedMessage.getFirst();
                exchange.setResponseContentLength(response.length);
                if (response.length == 0) {
                    exchange.endExchange();
                } else {
                    httpObjectSocket.storeLPMessage(nextQueuedMessage.getSecond(), response);

                    try {
                        exchange.send(new String(response,"UTF-8")); //FIXME: performance
                    } catch (UnsupportedEncodingException ex) {
                        Log.Error(this,ex);
                    }
                    exchange.endExchange();
                }
            },
            exchange
        );
    }

}
