package org.nustaq.kontraktor.remoting.websocket;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.ObjectSocket;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.util.Log;

import javax.websocket.*;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.function.Consumer;

/**
 * Created by ruedi on 29/03/15.
 *
 * Connect to a remotely websocket-published actor.
 *
 */
public class WebSocketClient<T extends Actor> extends ActorClient<T> {


    public static <AC extends Actor> Future<AC> Connect( Class<AC> clz, String adr, Consumer<Actor> disconnectHandler ) throws Exception {
        if ( disconnectHandler != null ) {
            disconnectHandler = Actors.InThread(disconnectHandler);
        }
        Promise<AC> res = new Promise<>();
        WebSocketClient<AC> client = new WebSocketClient<>( clz, adr);
        if ( disconnectHandler != null ) {
            client.setDisconnectHandler(disconnectHandler);
        }
        new Thread(() -> {
            try {
                client.connect();
                res.settle(client.getFacadeProxy(), null);
            } catch (IOException e) {
                Log.Info(WebSocketClient.class,null,""+e);
                res.settle(null, e);
            }
        }, "connection thread "+client.getDescriptionString()).start();
        return res;
    }

    @ClientEndpoint
    protected static class WSClientEndpoint {

        private final WebSocketClient con;
        protected WebObjectSocket sock;
        protected Session session = null;

        public WSClientEndpoint(URI endpointURI, WebObjectSocket sock, WebSocketClient con) {
            try {
                WebSocketContainer container = ContainerProvider.getWebSocketContainer();
                container.connectToServer(this, endpointURI);
                this.sock = sock;
                this.con = con;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        @OnOpen
        public void onOpen(Session userSession) {
            this.session = userSession;
        }

        @OnClose
        public void onClose(Session userSession, CloseReason reason) {
            this.session = null;
            try {
                sock.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @OnMessage
        public void onMessage(byte[] message) {
            try {
                sock.setNextMsg(message);
                while( con.singleReceive(sock) ) {
                    // do nothing
                }
            } catch (Exception e) {
                e.printStackTrace();
                // FIXME: cleanup ?
            }
        }

        @OnMessage
        public void onTextMessage(String message) {
        }

        public void sendText(String message) {
            session.getAsyncRemote().sendText(message);
        }

        public void sendBinary(byte[] message) {
            session.getAsyncRemote().sendBinary(ByteBuffer.wrap(message));
        }

    }

    String addr;

    public WebSocketClient(Class<T> clz, String addr) throws IOException {
        super(clz);
        this.addr = addr;
    }

    @Override
    protected ObjectSocket createObjectSocket() {
        WSClientEndpoint[] ep = new WSClientEndpoint[1];
        WebObjectSocket sock = new WebObjectSocket( getConf() ) {
            @Override
            public void writeObject(Object toWrite) throws Exception {
                ep[0].sendBinary(getConf().asByteArray(toWrite));
            }
        };
        try {
            ep[0] = new WSClientEndpoint(new URI(addr),sock,this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return sock;
    }

    protected String getDescriptionString() {
        return super.getDescriptionString() + "@" + addr;
    }

    public String getAddr() {
        return addr;
    }
}
