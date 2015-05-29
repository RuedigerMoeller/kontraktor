package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.JSR356ClientConnector;

/**
 * Created by ruedi on 19/05/15.
 *
 * describes a remotactor connectable via websockets
 *
 */
public class WebsocketConnectable implements ConnectableActor {

    Class clz;
    String url;
    Coding coding;

    public WebsocketConnectable(Class clz, String url) {
        this( clz, url, new Coding(SerializerType.FSTSer));
    }

    public WebsocketConnectable(Class clz, String url, Coding coding) {
        this.clz = clz;
        this.url = url;
        this.coding = coding;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return JSR356ClientConnector.Connect(clz,url,coding);
    }

}
