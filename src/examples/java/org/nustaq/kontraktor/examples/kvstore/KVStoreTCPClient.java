package org.nustaq.kontraktor.examples.kvstore;

import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;

/**
 * Created by ruedi on 21.08.2014.
 */
public class KVStoreTCPClient {

    public static void main(String a[]) throws IOException {
        TCPActorClient.Connect(KVStore.class, "localhost", 4444)
        .then((store, err) -> {
            try {
                store.$streamValues((r, e) -> System.out.println("streamed:"+r) );
                store.$put("TCP_Value", new SampleRecord(new URL("http://ruedigermoeller.github.io/"), 13, 1, "n/a"));
                store.$get("TCP_Value").then( (r, e) -> System.out.println(r) );
                store.$get("Null").then( (r,e) -> System.out.println("null:"+r) );

                Thread.sleep(2000);
                store.$stop(); // disconnects !
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    public static class SampleRecord implements Serializable {
        URL url;
        int hits;
        int unique;
        String desc;

        public SampleRecord(URL url, int hits, int unique, String desc) {
            this.url = url;
            this.hits = hits;
            this.desc = desc;
            this.unique = unique;
        }

        @Override
        public String toString() {
            return "SampleRecord{" +
                    "url=" + url +
                    ", hits=" + hits +
                    ", unique=" + unique +
                    ", desc='" + desc + '\'' +
                    '}';
        }
    }

}
