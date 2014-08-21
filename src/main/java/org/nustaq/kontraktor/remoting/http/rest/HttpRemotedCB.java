package org.nustaq.kontraktor.remoting.http.rest;

import org.nustaq.kontraktor.Callback;

/**
 * Created by ruedi on 21.08.2014.
 */
public class HttpRemotedCB implements Callback {

    int cbid;

    public HttpRemotedCB() {
    }

    public HttpRemotedCB(int cbid) {
        this.cbid = cbid;
    }

    public int getCbid() {
        return cbid;
    }

    public void setCbid(int cbid) {
        this.cbid = cbid;
    }

    @Override
    public void receiveResult(Object result, Object error) {

    }

}
