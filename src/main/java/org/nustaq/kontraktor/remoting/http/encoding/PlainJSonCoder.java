package org.nustaq.kontraktor.remoting.http.encoding;

import org.nustaq.kontraktor.remoting.RemoteCallEntry;

/**
 * Created by ruedi on 17.08.14.
 */
public class PlainJSonCoder extends KsonMsgCoder {
    public PlainJSonCoder(Class servingActor) {
        super(servingActor);
    }

    @Override
    public String encode(RemoteCallEntry resultOrCb) throws Exception {
        return kson.writePlainJSonObject(resultOrCb);
    }
}
