package org.nustaq.kontraktor.remoting.registry;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpClientConnector;

/**
 * Created by ruedi on 19/05/15.
 */
public class HttpConnectable implements ConnectableActor {

    Class clz;
    String url;
    Coding coding;
    HttpClientConnector.HttpClientConfig cfg;
    Object[] authData; // always json encoded

    public HttpConnectable(Class clz, String url) {
        this( clz, url, new Coding( SerializerType.FSTSer ) );
    }

    public HttpConnectable(Class clz, String url, Object[] authData) {
        this( clz,url, new Coding( SerializerType.FSTSer ), authData, HttpClientConnector.LONG_POLL );
    }

    public HttpConnectable(Class clz, String url, Coding coding) {
        this( clz,url, coding, new Object[] {"user","pwd"},HttpClientConnector.LONG_POLL );
    }

    public HttpConnectable(Class clz, String url, Object[] authData, Coding coding) {
        this( clz,url, coding, authData, HttpClientConnector.LONG_POLL );
    }

    public HttpConnectable(Class clz, String url, Coding coding, Object[] authData, HttpClientConnector.HttpClientConfig cfg) {
        this.clz = clz;
        this.url = url;
        this.coding = coding;
        this.cfg = cfg;
        this.authData = authData;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        return HttpClientConnector.Connect(clz,url,disconnectCallback,authData,coding,cfg);
    }

    public Class getClz() {
        return clz;
    }

    public String getUrl() {
        return url;
    }

    public Coding getCoding() {
        return coding;
    }

    public HttpClientConnector.HttpClientConfig getCfg() {
        return cfg;
    }
}
