package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.messagestore.MessageStore;
import org.nustaq.kontraktor.remoting.spa.FourK;
import org.nustaq.kontraktor.remoting.spa.FourKSession;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketErrorMessage;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketTextMessage;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.offheap.bytez.bytesource.ByteArrayByteSource;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by ruedi on 30.03.2015.
 */
public class WebSocketActorServerAdapter extends ActorServerAdapter {

    protected NioHttpServer server;
    protected Coding coding;
    public WebSocketActorServerAdapter(NioHttpServer server, Coding coding, Actor facade, boolean virtualConnection) {
        super(facade);
        this.coding = coding;
        this.server = server;
        this.virtualConnection = virtualConnection;
    }

    @Override
    public void acceptLoop() throws Exception {
        // event driven
    }

    @Override
    protected ActorServerConnection acceptConnections() throws Exception {
        return null; // event driven (see onOpen)
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
        channel.setAttribute("con", con);
        doAccept(con);
    }

    public void onBinaryMessage(WebSocketChannelAdapter channel, byte[] buffer) {
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        MyWSObjectSocket myWSObjectSocket = (MyWSObjectSocket) con.getObjectSocket().get();
        synchronized (myWSObjectSocket) { // defensive, should be single threaded
            myWSObjectSocket.setNextMsg(buffer);
            try {
                con.singleReceive(myWSObjectSocket);
            } catch (Exception e) {
                e.printStackTrace();
                // FIXME: cleanup ?
            }
        }
    }

    public void onClose(WebSocketChannelAdapter channel) {
        ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
        if ( ! virtualConnection )
            con.handleConnectionTermination();
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
        if ( ! virtualConnection ) {
            ActorServerConnection con = (ActorServerConnection) channel.getAttribute("con");
            con.handleConnectionTermination();
        }
    }

    public IPromise<byte[]> handleLongPoll(KontraktorHttpRequest req) {
        Promise res = new Promise();
        if ( req.getBinary() != null )
        {
            // long poll fallback for FourK server tweaked in ..
            String sessionId = req.getPath(req.getPathLen() - 1);
            if ( getFacade() instanceof FourK ) {
                ((FourK)getFacade()).$getSession(sessionId).then( (session,e) -> {
                    if ( session != null ) {
                        FourKSession fks = (FourKSession)session;
                        ActorServerConnection con = fks.getRegistry();

                        // same as send binary
                        MyWSObjectSocket myWSObjectSocket = (MyWSObjectSocket) con.getObjectSocket().get();
                        synchronized (myWSObjectSocket) { // defensive, should be single threaded
                            myWSObjectSocket.setNextMsg(req.getBinary());
                            try {
                                con.singleReceive(myWSObjectSocket);
                            } catch (Exception xe) {
                                Log.Warn(this, xe);
                            }
                        }

                        // read outgoing queue
//                        if ( myWSObjectSocket.toWrite.size() > 0 )
                        {
                            res.resolve(myWSObjectSocket.flushToArray());
                        }
                    } else {
                        // lp should go here
                        res.resolve(new byte[0]);
                    }
                });
            }
        }
        return res;
    }

    public class MyWSObjectSocket extends WebObjectSocket {
        WebSocketChannelAdapter channel;
        protected MessageStore sendHistory;
        volatile boolean closed = false;

        MyWSObjectSocket(FSTConfiguration conf, WebSocketChannelAdapter channel ) {
            super(conf);
            this.channel = channel;
        }

        @Override
        public void writeObject(Object o) throws Exception {
            super.writeObject(o);
        }

        @Override
        public void writeAndFlushObject(Object toWrite) throws Exception {
            if ( closed ) {
                throw new RuntimeException("channel is closed");
            }
            final byte[] b = conf.asByteArray(toWrite);
            if ( sendHistory != null ) {
                sendHistory.putMessage("dummy",writeSequence.get()-1, new ByteArrayByteSource(b));
            }
            channel.sendBinaryMessage(b);
        }

        public byte[] flushToArray() {
            synchronized (toWrite) {
                int size = toWrite.size();
                if (size==0)
                    return new byte[0];
                toWrite.add(writeSequence.getAndIncrement());
                Object[] objects = toWrite.toArray(); // fixme performance
                toWrite.clear();
                final byte[] b = conf.asByteArray(objects);
                if ( sendHistory != null ) {
                    sendHistory.putMessage("dummy",writeSequence.get()-1, new ByteArrayByteSource(b));
                }
                return b;
            }
        }

        @Override
        public void close() throws IOException {
            closed = true;
            channel.close();
        }

        @Override
        public FSTConfiguration getConf() {
            return conf;
        }

        public WebSocketChannelAdapter getChannel() {
            return channel;
        }

        @Override
        public boolean isClosed() {
            return channel == null || channel.isClosed();
        }

    }

}
