package org.nustaq.kontraktor.remoting.http;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ActorClient;
import org.nustaq.kontraktor.remoting.base.ActorClientConnector;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;

/**
 * Created by ruedi on 19/05/15.
 *
 * Default configuration is Long Poll, Binary Serialization
 * example:
 * <pre>
 * remoteApp = (MyHttpApp)
 *             new HttpConnectable(MyHttpApp.class, "http://localhost:8080/api")
 *                 .serType(SerializerType.JsonNoRefPretty)
 *                 .connect(null)
 *                 .await();
 * </pre>
 *
 */
public class HttpConnectable implements ConnectableActor {

    protected Class clz;
    protected String url;
    protected Coding coding = new Coding( SerializerType.FSTSer );
    protected Object[] authData; // always json encoded
    protected boolean noPoll = false;

    protected boolean shortPollMode = false;   // if true, do short polling instead
    protected long shortPollIntervalMS = 5000;

    public HttpConnectable() {
    }

    public HttpConnectable(Class clz, String url) {
        this.clz = clz;
        this.url = url;
    }

    public HttpConnectable noPoll(boolean noPoll) {
        this.noPoll = noPoll;
        return this;
    }

    public HttpConnectable shortPoll(boolean shortPollMode) {
        this.shortPollMode = shortPollMode;
        return this;
    }

    public HttpConnectable shortPollIntervalMS(long shortPollIntervalMS) {
        this.shortPollIntervalMS = shortPollIntervalMS;
        return this;
    }

    public HttpConnectable actorClazz(Class clz) {
        this.clz = clz;
        return this;
    }

    public HttpConnectable url(String url) {
        this.url = url;
        return this;
    }

    public HttpConnectable coding(Coding coding) {
        this.coding = coding;
        return this;
    }

    /**
     * overwrites settings made by 'coding'
     * @param type
     * @return
     */
    public HttpConnectable serType(SerializerType type) {
        this.coding = new Coding(type);
        return this;
    }

    public HttpConnectable authData(Object[] authData) {
        this.authData = authData;
        return this;
    }

    @Override
    public <T> IPromise<T> connect(Callback<ActorClientConnector> disconnectCallback) {
        HttpClientConnector con = new HttpClientConnector(this);
        con.disconnectCallback = disconnectCallback;
        ActorClient actorClient = new ActorClient(con, clz, coding);
        Promise p = new Promise();
        con.getRefPollActor().execute(() -> {
            Thread.currentThread().setName("Http Ref Polling");
            actorClient.connect().then(p);
        });
        return p;
    }

    @Override
    public ConnectableActor actorClass(Class actorClz) {
        clz = actorClz;
        return this;
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

    public Object[] getAuthData() {
        return authData;
    }

    public boolean isNoPoll() {
        return noPoll;
    }

    public boolean isShortPollMode() {
        return shortPollMode;
    }

    public long getShortPollIntervalMS() {
        return shortPollIntervalMS;
    }
}
