package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.ActorProxy;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.spa.FourKSession;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketErrorMessage;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketTextMessage;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by ruedi on 30.03.2015.
 */
public class WebSocketActorServer extends ActorServer {

    protected NioHttpServer server;
    protected Coding coding;

    public WebSocketActorServer( NioHttpServer server, Coding coding, Actor facade) {
        super(facade);
        this.coding = coding;
        this.server = server;
    }

    @Override
    public void acceptLoop() throws Exception {
        // event driven
    }

    @Override
    protected ActorServerConnection acceptConnections() throws Exception {
        return null; // event driven
    }

    /**
     * @return wether to spwan a thread per client
     */
    protected boolean isThreadPerClient() {
        return false;
    }

    @Override
    protected void connectServerSocket() throws Exception {
        // needs to be wired from outside
        throw new RuntimeException("unsupported operation");
    }

    @Override
    protected void closeSocket() {
        server.stopServer();
    }

    ///////////////////////////////////////////////// ws adapter callbacks

    public void onOpen(WebSocketChannelAdapter channel) {
        ActorServerConnection con = new ActorServerConnection(coding);
        MyWSObjectSocket socket = new MyWSObjectSocket(con.getConf(), channel);
        con.init(socket, facade);
        channel.setAttribute("con",con);
        doAccept(con);
    }

    public void onBinaryMessage(WebSocketChannelAdapter channel, byte[] buffer) {
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        ((MyWSObjectSocket)con.getObjSocket()).setNextMsg(buffer);
        con.currentObjectSocket.set(con.getObjSocket());
        try {
            while( con.singleReceive(con.getObjSocket()) ) {
                // do nothing
            }
        } catch (Exception e) {
            e.printStackTrace();
            // FIXME: cleanup ?
        }
    }

    public void onClose(WebSocketChannelAdapter channel) {
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        con.handleTermination();
    }

    public void onTextMessage(WebSocketChannelAdapter channel, String text) {
        if ( text.equals("KTR_PING") ) {
            return;
        }
        // kontrakor uses binary messages only, however just forward text messages sent by a client
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        facade.$receive(new WebSocketTextMessage(text, channel));
    }

    public void onPong(WebSocketChannelAdapter channel) {
        // ping pong is implemented by adapter
    }

    public void onError(WebSocketChannelAdapter channel) {
        Log.Lg.info(this, "websocket connection error " + channel);
        facade.$receive(new WebSocketErrorMessage(channel));
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        con.handleTermination();
    }

    public IPromise<byte[]> handleLongPoll(KontraktorHttpRequest req) {
        if ( req.getBinary() != null  )
        {
            if ( req.getBinary().length == 4 ) { // indicates sequence of empty longoll req. somewhat quirksy, however any serialized obj will be longer
                // todo:
                // read up to N messages from messages store and send response.
                // sending of messages has to also be redirected to message store
                // optionally (no reliability) just poll queue and send results back.
                //
                // need a per session WebObjectSocket with patched write method
                // as the original socket is dead (in case of fallback). Can be created on
                // demand here (similar to onOpen), need strat to time them out though
                //
                ActorServerConnection con = (ActorServerConnection) req.getSession();
                return new Promise<>(new byte[0]);
            } else {
                //
                ActorServerConnection con = (ActorServerConnection) req.getSession();
                ((MyWSObjectSocket)con.getObjSocket()).setNextMsg(req.getBinary());
                con.currentObjectSocket.set(con.getObjSocket());
                try {
                    while( con.singleReceive(con.getObjSocket()) ) {
                        // do nothing
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    // FIXME: cleanup ?
                }
            }
        }
        return new Promise<>(new byte[0]);
    }

    class MyWSObjectSocket extends WebObjectSocket {
        WebSocketChannelAdapter channel;

        MyWSObjectSocket(FSTConfiguration conf, WebSocketChannelAdapter channel ) {
            super(conf);
            this.channel = channel;
        }

        @Override
        public void writeAndFlushObject(Object toWrite) throws Exception {
            final byte[] b = conf.asByteArray(toWrite);
            channel.sendBinaryMessage(b);
        }

        @Override
        public void close() throws IOException {
            channel.close();

        }

        @Override
        public FSTConfiguration getConf() {
            return conf;
        }

    }

}
