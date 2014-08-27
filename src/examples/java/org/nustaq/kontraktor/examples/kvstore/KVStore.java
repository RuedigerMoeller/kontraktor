package org.nustaq.kontraktor.examples.kvstore;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.http.NioHttpServer;
import org.nustaq.kontraktor.remoting.http.NioHttpServerImpl;
import org.nustaq.kontraktor.remoting.http.rest.RestActorServer;
import org.nustaq.kontraktor.remoting.tcp.TCPActorServer;
import org.nustaq.offheap.FSTAsciiStringOffheapMap;
import org.nustaq.serialization.FSTConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

/**
 * Created by ruedi on 20.08.14.
 *
 * Implements a persistent key value storage service exported via
 * - tcp
 * - json webservice
 */
public class KVStore extends Actor<KVStore> {

    FSTConfiguration conf;
    FSTAsciiStringOffheapMap store;

    public Future $init( int keylen, int sizeGB, String file ) {
        try {
            conf = FSTConfiguration.createDefaultConfiguration();
            store = new FSTAsciiStringOffheapMap(file, keylen, sizeGB * FSTAsciiStringOffheapMap.GB, 10*1000000, conf);
        } catch (Exception e) {
            e.printStackTrace();
            return new Promise<>(null,e);
        }
        return new Promise("void");
    }

    public Future $sync() {
        return new Promise<>("void");
    }

    public Future $get(String key) {
        return new Promise<>(store.get(key));
    }

    public void $put( String key, Serializable value) {
        store.put(key,value);
    }

    public void $putIfAbsent(String key, Serializable value) {
        if ( store.get(key) != null ) {
            store.put(key, value);
        }
    }

    public Future<Integer> getFreeMemMB() {
        return new Promise<>((int)(store.getFreeMem()/1024/1024));
    }

    public void $stream( Spore spore ) {
        store.values().forEachRemaining((v) -> {
            spore.remote(v);
        });
        spore.remote(Callback.FIN);
    }

    @Override
    public void $stop() {
        super.$stop();
        store.free();
    }

    public static void main(String arg[]) {

        // create Service Actor
        KVStore service = Actors.AsActor(KVStore.class);

        // figure file location for persistence
        String file = "kvstore.mmf";
        if ( new File("/tmp").exists() ) file = "/tmp/kvstore.mmf";

        // start service
        service.$init(32, 32, file).then( (r,e) -> {
            if ( e instanceof Throwable ) {
                ((Throwable) e).printStackTrace();
            } else {
                // start TCP service
                try {
                    TCPActorServer.Publish(service,4444);
                } catch (IOException e1) {
                    e1.printStackTrace();
                }

                // start Http service
                RestActorServer sv = new RestActorServer();

                // a netty based impl is available separately, use internal server here ..
                NioHttpServer server = Actors.AsActor(NioHttpServerImpl.class, 64000);
                sv.startOnServer(9999, server);

                // publish service actor
                sv.publish("kvstore",service);
            }
        });
    }

}
