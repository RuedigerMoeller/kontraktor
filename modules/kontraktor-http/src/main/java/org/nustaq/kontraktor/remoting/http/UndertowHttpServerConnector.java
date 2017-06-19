/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.http;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.Methods;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.kontraktor.util.Pair;
import org.xnio.channels.StreamSinkChannel;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Created by ruedi on 12.05.2015.
 *
 * A longpoll+shortpoll+nopoll based connector. Binary/MinBin/Json coding using POST requests is supported
 *
 * Algorithm/Expected client behaviour:
 *
 * Longpoll request is held until an event occurs or timeout. An incoming lp request reports
 * last seen sequence in its url ../sessionId/sequence such that in case of network failure
 * messages can be delivered more than once from a short (outgoing) message history.
 *
 * Regular requests are expected to come in ordered (so next request is done only if first one was replied).
 * This is necessary due to http 1.1 limitations (multiple connections+reordering would be required otherwise).
 * Responses to regular requests (actor messages) are piggy backed with long-poll-data if avaiable. This may
 * lead to out of sequence long polls (network race). So a client has to implement sequence checking in order
 * to prevent double processing of incoming messages.
 *
 * For shortpoll, a client sends "{ 'SP', sequence }" to indicate the poll request should return immediately.
 * With many clients and connection limited clients (browser,mobiles) a short poll with larger intervals (>3s) might scale better
 * at cost of latency.
 *
 * When used no-poll, streaming results to a callback is not supported. Only 'tellMsg' (void methods) and 'askMsg' (IPromise-returning)
 * messages can be used.
 *
 * ATTENTION: parts of stuff was pulled up into an abstract class
 *
 * TODO: support temporary/discardable websocket connections as a LP optimization.
 * TODO: investigate http 2.0
 */
public class UndertowHttpServerConnector extends AbstractHttpServerConnector implements HttpHandler {

    public UndertowHttpServerConnector(Actor facade) {
        super(facade);
    }

    /**
     * if relpath is empty, this is considered as a connect/open and a session id is sent back.
     * Else /sessionId/sequ or just /sessionId is expected
     * posted content might contain an authentication information related object
     *
     * @param exchange
     * @throws Exception
     */
    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        if ( exchange.getRequestMethod() != Methods.POST ) {
            exchange.setResponseCode(404);
            exchange.endExchange();
            return;
        }
        String rpath = exchange.getRelativePath();

        StreamSourceChannel requestChannel = exchange.getRequestChannel();
        String first = exchange.getRequestHeaders().getFirst(Headers.CONTENT_LENGTH);
        int len = Integer.parseInt(first);

        // read post data.
        ByteBuffer buf = ByteBuffer.allocate(len);
        requestChannel.getReadSetter().set( streamSourceChannel -> {
                try {
                    streamSourceChannel.read(buf);
                } catch (IOException e) {
                    Log.Warn(this,e);
                }
                if ( buf.remaining() == 0 ) {
                    try {
                        requestChannel.shutdownReads();
                    } catch (IOException e) {
                        Log.Warn(this,e);
                    }
                    facade.execute( () -> requestReceived( exchange, buf.array(), rpath ) );
                }
            }
        );
        requestChannel.resumeReads();
    }

    protected void requestReceived( HttpServerExchange exchange, byte[] postData, String path) {
        // already executed in facade thread
        while ( path.startsWith("/") )
            path = path.substring(1);

        if ( path.trim().length() > 0 ) {
            String[] split = path.split("/");
            HttpObjectSocket httpObjectSocket = sessions.get(split[0]);
            if ( httpObjectSocket != null ) {
                handleClientRequest(exchange, httpObjectSocket, postData, split.length > 1 ? split[1] : null);
            } else {
                httpObjectSocket = restoreSessionFromId(split[0]);
                if ( httpObjectSocket != null ) {
                    handleClientRequest(exchange, httpObjectSocket, postData, split.length > 1 ? split[1] : null);
                } else {
                    exchange.setResponseCode(404);
                    exchange.endExchange();
                }
            }
        } else { // new session
            Object auth = null;
            // create connection. postdata is auth data
            if ( postData != null && postData.length > 0 ) {
                auth = conf.asObject(postData);
            }

            // auth check goes here

            handleNewSession(new UndertowKHttpExchangeImpl(exchange));
        }
    }

    public void handleClientRequest(HttpServerExchange exchange, HttpObjectSocket httpObjectSocket, byte[] postData, String lastSeenSequence) {

        // dispatch incoming messages to actor(s)
        StreamSinkChannel sinkchannel = exchange.getResponseChannel();
        if ( sinkchannel == null ) {
            Log.Error(this,"could not aquire response channel. rejecting request.");
            exchange.endExchange();
            return;
        }

        // executed in facade thread
        httpObjectSocket.updateTimeStamp(); // keep alive

        Object received[] = (Object[]) httpObjectSocket.getConf().asObject(postData);

        boolean isEmptyLP = received.length == 1 && received[0] instanceof Number;


        if ( ! isEmptyLP ) {
            handleRegularRequest(exchange, httpObjectSocket, received, sinkchannel);
            return;
        }

        // long poll request
        // parse sequence
        int lastClientSeq = -1;
        if ( lastSeenSequence!=null ) {
            try {
                lastClientSeq = Integer.parseInt(lastSeenSequence);
            } catch (Throwable t) {
                Log.Warn(this,t);
            }
        }

        // check if can be served from history
        if (lastClientSeq > 0 ) { // if lp response message has been sent, take it from history
            byte[] msg = (byte[]) httpObjectSocket.takeStoredLPMessage(lastClientSeq + 1);
            if (msg!=null) {
//                Log.Warn(this, "serve lp from history " + (lastClientSeq + 1) + " cur " + httpObjectSocket.getSendSequence());
                replyFromHistory(exchange, sinkchannel, msg);
                return;
            }
        }

        // new longpoll request ..

        sinkchannel.resumeWrites();
        // read next batch of pending messages from binary queue and send them
        Pair<Runnable,KHttpExchange> lpTask = createLongPollTask( new UndertowKHttpExchangeImpl(exchange), httpObjectSocket, sinkchannel);
        // release previous long poll request if present
        httpObjectSocket.cancelLongPoll();
        httpObjectSocket.setLongPollTask(lpTask);
    }

    protected Pair<Runnable,KHttpExchange> createLongPollTask(KHttpExchange exchange, HttpObjectSocket httpObjectSocket, StreamSinkChannel sinkchannel) {
        return new Pair<>(
            () -> {
                if ( ! sinkchannel.isOpen() )
                    return;
                Pair<byte[], Integer> nextQueuedMessage = httpObjectSocket.getNextQueuedMessage();
                byte response[] = nextQueuedMessage.getFirst();
                exchange.setResponseContentLength(response.length);
                if (response.length == 0) {
                    exchange.endExchange();
                } else {
                    httpObjectSocket.storeLPMessage(nextQueuedMessage.getSecond(), response);


                    // FIXME: should be async ?
                    exchange.send(response);
//                    ByteBuffer responseBuf = ByteBuffer.wrap(response);
//                    try {
//                        while (responseBuf.remaining()>0) {
//                            sinkchannel.write(responseBuf);
//                        }
//                    } catch (Throwable e) {
//                        System.out.println("buffer size:"+response.length);
//                        try {
//                            sinkchannel.close();
//                        } catch (IOException e1) {
//                            e1.printStackTrace();
//                        }
//                        e.printStackTrace();
//                    }
//                    exchange.endExchange();
                }
            },
            exchange
        );
    }

    protected void replyFromHistory(HttpServerExchange exchange, StreamSinkChannel sinkchannel, byte[] msg) {
        ByteBuffer responseBuf = ByteBuffer.wrap(msg);
        exchange.setResponseContentLength(msg.length);
        sinkchannel.getWriteSetter().set(
            channel -> {
                if (responseBuf.remaining() > 0)
                    try {
                        sinkchannel.write(responseBuf);
                        if (responseBuf.remaining() == 0) {
                            exchange.endExchange();
                        } else
                            sinkchannel.resumeWrites(); // required ?
                    } catch (Exception e) {
                        e.printStackTrace();
                        exchange.endExchange();
                    }
                else {
                    exchange.endExchange();
                }
            }
        );
        sinkchannel.resumeWrites();
    }

    /**
     * handle a remote method call (not a long poll)
     *
     * @param exchange
     * @param httpObjectSocket
     * @param received
     * @param sinkchannel
     */
    protected void handleRegularRequest(HttpServerExchange exchange, HttpObjectSocket httpObjectSocket, Object[] received, StreamSinkChannel sinkchannel) {
        ArrayList<IPromise> futures = new ArrayList<>();
        httpObjectSocket.getSink().receiveObject(received, futures, exchange.getRequestHeaders().getFirst("JWT") );

        Runnable reply = () -> {
            // piggy back outstanding lp messages, outstanding lp request is untouched
            Pair<byte[], Integer> nextQueuedMessage = httpObjectSocket.getNextQueuedMessage();
            byte response[] = nextQueuedMessage.getFirst();
            exchange.setResponseContentLength(response.length);
            if (response.length == 0) {
                exchange.endExchange();
            } else {
                httpObjectSocket.storeLPMessage(nextQueuedMessage.cdr(), response);

                //FIXME: ASYNC !!!
                long tim = System.nanoTime();
                ByteBuffer responseBuf = ByteBuffer.wrap(response);
                try {
                    while (responseBuf.remaining()>0) {
                        sinkchannel.write(responseBuf);
                    }
                } catch (IOException e) {
                    Log.Warn(this,e);
                }
//                System.out.println("syncwrite time micros:"+(System.nanoTime()-tim)/1000);
                exchange.endExchange();
            }
        };
        if ( futures == null || futures.size() == 0 ) {
            reply.run();
        } else {
            Actors.all((List) futures).timeoutIn(REQUEST_RESULTING_FUTURE_TIMEOUT).then( () -> {
                reply.run();
            }).onTimeout( () -> reply.run() );
            sinkchannel.resumeWrites();
        }
    }

}
