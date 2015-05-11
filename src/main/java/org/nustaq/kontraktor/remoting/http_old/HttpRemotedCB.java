package org.nustaq.kontraktor.remoting.http_old;

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
    public void complete(Object result, Object error) {

    }

}
