package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.base.ActorServerAdapter;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.messagestore.HeapMessageStore;
import org.nustaq.kontraktor.remoting.messagestore.MessageStore;
import org.nustaq.kontraktor.remoting.spa.FourK;
import org.nustaq.kontraktor.remoting.spa.FourKSession;
import org.nustaq.kontraktor.remoting.websocket.adapter.LongPollSocketChannelAdapter;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketChannelAdapter;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketErrorMessage;
import org.nustaq.kontraktor.remoting.websocket.adapter.WebSocketTextMessage;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.offheap.bytez.ByteSource;
import org.nustaq.offheap.bytez.bytesource.ByteArrayByteSource;
import org.nustaq.offheap.bytez.onheap.HeapBytez;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.minbin.MinBin;

import java.io.IOException;

/**
 * Created by ruedi on 30.03.2015.
 */
public class WebSocketActorServerAdapter extends ActorServerAdapter {

    public static final byte[] UNKNOWN = FSTConfiguration.createMinBinConfiguration().asByteArray(null);

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
        // comes in XNIO thread
        Promise res = new Promise();
        if ( req.getBinary() != null )
        {
            // long poll fallback for FourK server tweaked in ..
            // temporary non-actor based implementation. TODO: consider handling long polls from a dedicated actor
            String sessionId = req.getPath(req.getPathLen() - 1);
            if ( getFacade() instanceof FourK ) {
                ((FourK)getFacade()).$getSession(sessionId).then( (session,e) -> {
                    // runs in main session dispatcher
                    if ( session != null ) {
                        FourKSession fks = (FourKSession)session;
                        ActorServerConnection con = fks.__getRegistry();
                        if ( con == null ) {
                            // fake websocket open
                            con = new ActorServerConnection(coding);
                            LongPollSocketChannelAdapter channel = new LongPollSocketChannelAdapter();
                            MyWSObjectSocket socket = new MyWSObjectSocket(con.getConf(), channel);
                            con.init(socket, facade);
                            channel.setAttribute("con", con);
                            doAccept(con);
                            fks.__setRegistry(con);
                        }

                        // same as send binary
                        MyWSObjectSocket myWSObjectSocket = (MyWSObjectSocket) con.getObjectSocket().get();

                        if ( req.getBinary().length > 5 ) { // not a long poll
                            myWSObjectSocket.setNextMsg(req.getBinary());
                            try {
                                con.singleReceive(myWSObjectSocket);
                            } catch (Exception xe) {
                                Log.Warn(this, xe);
                            }
                            res.complete();
                        } else {
                            Object sequence = myWSObjectSocket.getConf().asObject(req.getBinary());
                            long lseq;
                            if ( sequence instanceof Number ) {
                                lseq = ((Number)sequence).longValue();
                            } else
                                lseq = 0;
                            // result will already be enqueued for majority of cases
                            Actors.SubmitPeriodic(100, tim -> {
                                synchronized (myWSObjectSocket) {
                                    // read outgoing queue
                                    byte[] result = myWSObjectSocket.flushToArray(lseq);
                                    if (result.length > 0) {
                                        res.resolve(result);
                                        return null;
                                    } else {
                                        if ( tim < 500 )
                                            return tim+100;
                                        if ( tim > 510 ) {
                                            res.resolve(null);
                                            return null;
                                        }
                                        return tim+1;
                                    }
                                }
                            });
                        }
                    } else {
                        // session is null
                        res.resolve(UNKNOWN);
                    }
                });
            }
        } else {
            res.resolve(new byte[0]);
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

        @Override
        public void flush() throws Exception {
            // don't flush for long polling as data is actually pulled (see flushToArray)
            if ( channel instanceof LongPollSocketChannelAdapter )
                return;
            super.flush();
        }

        public byte[] flushToArray(long clientSequence) {
            synchronized (toWrite) {
                if ( sendHistory == null ) {
                    sendHistory = new HeapMessageStore(10);
                }
                if ( clientSequence > 0 && clientSequence != writeSequence.get()-1 ) {
                    Log.Warn(this, "sequence gap my:"+writeSequence.get()+" client:"+clientSequence);
                    ByteSource def = sendHistory.getMessage("DEF", clientSequence+1);
                    if ( def != null ) {
                        // FIXME: does not hold true for all possible implementations of sendHistory
                        return ((ByteArrayByteSource)def).getArr();
                    } else {
                        Log.Lg.error(this,null,"unrecoverable sequence gap "+writeSequence.get()+" client:"+clientSequence);
                    }
                }
                int size = toWrite.size();
                if (size==0)
                    return new byte[0];
                int newSequence = writeSequence.getAndIncrement();
                toWrite.add(newSequence);
                Object[] objects = toWrite.toArray(); // fixme performance
                toWrite.clear();
                final byte[] b = conf.asByteArray(objects);
                System.out.println("lp flushing "+newSequence+" size:"+b.length);
                if ( sendHistory != null ) {
                    sendHistory.putMessage("DEF",newSequence, new ByteArrayByteSource(b));
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
