package org.nustaq.kontraktor.remoting.http_old.encoding;

import org.nustaq.kontraktor.remoting.encoding.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http_old.ArgTypesResolver;
import org.nustaq.kontraktor.remoting.http_old.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http_old.HttpMsgCoder;
import org.nustaq.kontraktor.remoting.http_old.HttpRemotedCB;
import org.nustaq.kson.Kson;

/**
 * Created by ruedi on 17.08.14.
 */
public class KsonMsgCoder implements HttpMsgCoder {

    Kson kson;
    ArgTypesResolver resolver;

    public KsonMsgCoder( Class servingActor ) {
        resolver = new ArgTypesResolver( servingActor );
        kson = new Kson().supportJSon(true)
                .map("call", RemoteCallEntry.class)
                .map("calls", RemoteCallEntry[].class)
                .map("rcb", HttpRemotedCB.class);
        kson.getMapper().setUseSimplClzName(false);
    }

    public Kson getKson() {
        return kson;
    }

    public KsonMsgCoder map( String s, Class clz ) {
        kson.map(s,clz);
        return this;
    }

    public KsonMsgCoder map( Class ... clz ) {
        kson.map(clz);
        return this;
    }

    @Override
    public RemoteCallEntry[] decodeFrom(String s, KontraktorHttpRequest req) throws Exception {
        Object calls = kson.readObject(s, "calls", resolver);
        if ( calls instanceof RemoteCallEntry ) {
            return new RemoteCallEntry[] {(RemoteCallEntry) calls};
        }
        return (RemoteCallEntry[]) calls;
    }

    @Override
    public String encode(RemoteCallEntry resultOrCb) throws Exception {
        return kson.writeObject(resultOrCb,false);
    }

}
