package newimpl;

import org.junit.Test;
import org.nustaq.reallive.newimpl.*;

/**
 * Created by ruedi on 04.08.2015.
 */
public class Basic {

    @Test
    public void test() {
        ChangeReceiverImpl source = new ChangeReceiverImpl(new HeapStore<>());
        
        ChangeStreamImpl<String,Record<String>> stream = new ChangeStreamImpl(source.getStore());
        source.listener(stream);

        stream.subscribe(
            new Subscriber<>(
                record -> "one".equals(record.getKey()),
                change -> { System.out.println("listener"+change); }
            )
        );

        ChangeRequestBuilder cb = ChangeRequestBuilder.get();
        source.receive(cb.add("one",
                                 "name", "emil",
                                 "age", 9
        ));
        source.receive(cb.add("two",
                                 "name", "felix",
                                 "age", 17
        ));
        source.receive(cb.update("one", "age", 10));
        source.receive(cb.remove("one"));
        source.getStore().forEach( rec -> {
            System.out.println(rec);
        });
    }

    @Test
    public void bench() {
        long tim = System.currentTimeMillis();
        for ( int ii = 0; ii < 100; ii++) {
            ChangeRequestBuilder cb = ChangeRequestBuilder.get();
            ChangeReceiverImpl stream = new ChangeReceiverImpl(new HeapStore<>());
            stream.listener( change -> {
                //System.out.println(change);
            });
            tim = System.currentTimeMillis();
            for ( int i = 0; i < 100_000; i++ ) {
                stream.receive(cb.add("one"+i,
                    "name", "emil",
                    "age", 9,
                    "bla", 13,
                    "y", 123.45,
                    "y1", 123.45,
                    "y2", 123.45,
                    "y3", 123.45,
                    "y4", 123.45,
                    "y5", 123.45,
                    "y6", 123.45,
                    "y7", 123.45,
                    "y8", 123.45,
                    "y9", 123.45
                ));
            }
            System.out.println("ADD "+(System.currentTimeMillis()-tim) );
            tim = System.currentTimeMillis();
            for ( int i = 0; i < 100_000; i++ ) {
                stream.receive(cb.update("one" + i, "age", 10));
            }
            System.out.println("UPD "+(System.currentTimeMillis()-tim) );
            tim = System.currentTimeMillis();
            for ( int i = 0; i < 100_000; i++ ) {
                stream.receive(cb.remove("one"+i) );
            }
            System.out.println("DEL "+(System.currentTimeMillis()-tim) );
        }
    }

}
