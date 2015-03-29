package org.nustaq.kontraktor.remoting.http.websocket;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.RemoteRefRegistry;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

/**
 * implements mechanics of transparent actor remoting via websockets based on the websocket api
 * inherited.
 *
 */
public class WebSocketEndpoint {

    public static int PING_INTERVAL_MILLIS = 10000;
    protected Actor facade; // ref to public application actor
    RemoteRefRegistry registry;

    Coding coding;

    public WebSocketEndpoint(Coding coding, Actor facade ) {
        this.facade = facade;
        this.coding = coding;
        registry = new RemoteRefRegistry(coding) {
	        @Override
	        public Actor getFacadeProxy() {
	            return facade;
	        }
        };
	    registry.publishActor(facade);
    }

    public void onOpen(WebSocketChannelAdapter channel) {
        MyWSObjectSocket socket = new MyWSObjectSocket(registry.getConf(), channel);
        channel.setAttribute("socket",socket);
    }

    public void onBinaryMessage(WebSocketChannelAdapter channel, byte[] buffer) {
        MyWSObjectSocket sock = (MyWSObjectSocket) channel.getAttribute("socket");
        sock.setNextMsg(buffer);
        try {
            while( registry.singleReceive(sock) ) {
                // do nothing
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onClose(WebSocketChannelAdapter channel) {
        MyWSObjectSocket sock = (MyWSObjectSocket) channel.getAttribute("socket");
        // FIXME: remove remote refs for this channel
        System.out.println("onClose");
    }

    public void onTextMessage(WebSocketChannelAdapter channel, String text) {
        // kontrakor uses binary messages only, however just forward text messages sent by a client
        facade.$receive(new WebSocketTextMessage(text, channel));
    }

    public void onPong(WebSocketChannelAdapter channel) {
        // ping pong is implemented by adapter
    }

    public void onError(WebSocketChannelAdapter channel) {
        // kontrakor uses binary messages only, however just forward text messages sent by a client
        // to facade actor
        Log.Lg.info(this, "websocket connection error " + channel);
        facade.$receive(new WebSocketErrorMessage(channel));
    }

    class MyWSObjectSocket extends WebObjectSocket {
        WebSocketChannelAdapter channel;

        MyWSObjectSocket(FSTConfiguration conf, WebSocketChannelAdapter channel ) {
            super(conf);
            this.channel = channel;
        }

        @Override
        public void writeObject(Object toWrite) throws Exception {
            final byte[] b = conf.asByteArray(toWrite);
            channel.sendBinaryMessage(b);
        }

        @Override
        public FSTConfiguration getConf() {
            return conf;
        }

    }

}
