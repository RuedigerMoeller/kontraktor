package org.nustaq.kontraktor.examples.kvstore;

import org.nustaq.kontraktor.remoting.http.rest.RestActorClient;
import org.nustaq.kontraktor.remoting.http.rest.RestActorServer;

import java.awt.*;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 21.08.2014.
 */
public class KVStoreWebClient {

    public static void main(String a[]) {
        RestActorClient<KVStore> cl = new RestActorClient("localhost", 9999, "/kvstore", KVStore.class).connect();

        KVStore proxy = cl.getFacadeProxy();

        for (int i=0; i<10; i++) {
            proxy.$put( "HelloHttp"+i, new Object[] { "from Http client", new Point(i,2*i) } );
        }
        proxy.$get("HelloHttp7").then((r, e) -> System.out.println("future:" + r));
        LockSupport.parkNanos(1000*1000*50);
        proxy.$get("HelloHttp6").then((r, e) -> System.out.println("future:" + r));
    }
}
