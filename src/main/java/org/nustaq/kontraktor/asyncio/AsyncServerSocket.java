package org.nustaq.kontraktor.asyncio;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.util.Log;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.function.BiFunction;

/**
 * Created by ruedi on 04/05/15.
 */
public class AsyncServerSocket {

    ServerSocketChannel socket;
    Selector selector;
    SelectionKey serverkey;
    BiFunction<SelectionKey,SocketChannel,AsyncServerSocketConnection> connectionFactory;

    public void connect( int port, BiFunction<SelectionKey,SocketChannel,AsyncServerSocketConnection> connectionFactory ) throws IOException {
        selector = Selector.open();
        socket = ServerSocketChannel.open();
        socket.socket().bind(new java.net.InetSocketAddress(port));
        socket.configureBlocking(false);
        serverkey = socket.register(selector, SelectionKey.OP_ACCEPT);
        this.connectionFactory = connectionFactory;
        receiveLoop();
    }

    public void receiveLoop() {
        Actor actor = Actor.sender.get();
        if ( actor == null )
        {
            throw new RuntimeException("only usable from within an actor");
        }
        try {
            selector.selectNow();
            for (Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                SelectionKey key = iterator.next();
                try {
                    if (key == serverkey) {
                        if (key.isAcceptable()) {
                            SocketChannel accept = socket.accept();
                            if (accept != null) {
                                accept.configureBlocking(false);
                                SelectionKey newKey = accept.register(selector, SelectionKey.OP_READ);
                                AsyncServerSocketConnection con = connectionFactory.apply(key, accept);
                                newKey.attach(con);
                            }
                        }
                    } else {
                        SocketChannel client = (SocketChannel) key.channel();
                        if (key.isReadable()) {
                            iterator.remove();
                            AsyncServerSocketConnection con = (AsyncServerSocketConnection) key.attachment();
                            if ( con == null ) {
                                System.out.println("con is null "+key);
                            } else {
                                actor.execute(() -> {
                                    try {
                                        con.readData();
                                    } catch (Exception ioe) {
                                        con.closed(ioe);
                                        key.cancel();
                                        try {
                                            client.close();
                                        } catch (IOException e) {
                                            Log.Warn(this, e);
                                        }
                                    }
                                });
                            }
                        }
                    }
                } catch (Throwable e) {
                    Log.Warn(this,e,"");
                }
            }
        } catch (Throwable e) {
            Log.Warn(this,e,"");
            Actors.reject(e);
        }
        if ( ! isClosed() ) {
            actor.delayed( 5, () -> receiveLoop() );
        }
    }

    public boolean isClosed() {
        return false;
    }


    public static class TA extends Actor<TA> {

        public void $serve() {
            AsyncServerSocket sock = new AsyncServerSocket();
            try {
                sock.connect( 8080, (key,con) -> new AsyncServerSocketConnection(key,con) );
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public static void main( String a[] ) throws InterruptedException {
        Actors.AsActor(TA.class).$serve();
        Thread.sleep(1000000l);
    }
}
