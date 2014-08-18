package org.nustaq.kontraktor.remoting.http.rest.encoding;

import org.nustaq.kontraktor.remoting.RemoteCallEntry;
import org.nustaq.kontraktor.remoting.http.ArgTypesResolver;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequest;
import org.nustaq.kontraktor.remoting.http.KontraktorHttpRequestImpl;
import org.nustaq.kontraktor.remoting.http.rest.HttpMsgCoder;
import org.nustaq.kson.Kson;

/**
 * Created by ruedi on 17.08.14.
 */
public class KsonMsgCoder implements HttpMsgCoder {

    Kson kson;
    ArgTypesResolver resolver;

    public KsonMsgCoder( Class servingActor ) {
        resolver = new ArgTypesResolver( servingActor );
        kson = new Kson().map("call", RemoteCallEntry.class).map("calls", RemoteCallEntry[].class);
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
