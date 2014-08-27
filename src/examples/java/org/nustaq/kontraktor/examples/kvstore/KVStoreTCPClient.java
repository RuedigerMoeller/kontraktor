package org.nustaq.kontraktor.examples.kvstore;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;

/**
 * Created by ruedi on 21.08.2014.
 */
public class KVStoreTCPClient {

    public static void main(String a[]) throws IOException {
        TCPActorClient.Connect(KVStore.class, "localhost", 4444)
        .then((store, err) -> {
            try {
//                store.$streamValues((r, e) -> System.out.println("streamed:"+r) );
                long now = System.currentTimeMillis();
                for ( int i = 0; i < 100000; i++ ) {
                    store.$put("TCP_Value_"+i, new SampleRecord(new URL("http://ruedigermoeller.github.io/"), i, 1, "n/a"));
                }
                store.$sync().then((r,e) -> System.out.println( "Time:"+(System.currentTimeMillis()-now) ) );

                store.$get("TCP_Value_1").then((r, e) -> System.out.println("TCP_Value_1:" + r));
                store.$get("Null").then( (r,e) -> System.out.println("null:"+r) );

                store.$stream(
                    new Spore() {

                        int hits;

                        {
                            hits = 0;
                        }

                        @Override
                        public void remote(Object input) {
                            if ( input instanceof SampleRecord ) {
                                if ( ((SampleRecord) input).getHits() > 10 && ((SampleRecord) input).getHits() < 100 ) {
                                    System.out.println("remote " + input);
                                    receive(input, Callback.CONT);
                                    hits++;
                                }
                            } else {
                                System.out.println("no match "+input);
                                if ( Actor.Fin(input) ) {
                                    receive(hits, Actor.FIN);
                                }
                            }
                        }

                        @Override
                        public void local(Object result, Object error) {
                            if ( Actor.Fin(error) ) {
                                System.out.println("Hits:"+result);
                            }
                            System.out.println("local received match "+result);
                        }
                    }
                );

                store.$stream(
                    new Spore() {

                        int count = 0;

                        @Override
                        public void remote(Object input) {
                            if (Actor.Fin(input)) {
                                receive(count, Actor.FIN);
                            } else {
                                count++;
                            }
                        }

                        @Override
                        public void local(Object result, Object error) {
                            System.out.println("Number of entries "+result);
                        }
                    }
                );

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

        public URL getUrl() {
            return url;
        }

        public int getHits() {
            return hits;
        }

        public int getUnique() {
            return unique;
        }

        public String getDesc() {
            return desc;
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
