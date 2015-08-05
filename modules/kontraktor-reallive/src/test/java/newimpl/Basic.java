package newimpl;

import org.junit.Test;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.*;
import org.nustaq.reallive.storage.*;

/**
 * Created by ruedi on 04.08.2015.
 */
public class Basic {

    @Test
    public void testOffHeap() {
        StorageDriver source = new StorageDriver(new OffHeapRecordStorage<>(32,500,600_000));
        insertTest(source);
    }

    public void insertTest(StorageDriver source) {FilterProcessor<String,Record<String>> stream = new FilterProcessor(source.getStore());
        source.setListener(stream);

        stream.subscribe(
            record -> "one13".equals(record.getKey()),
            change -> System.out.println("listener: " + change)
        );

        Mutation mut = source;
        long tim = System.currentTimeMillis();
        for ( int i = 0; i<500_000;i++ ) {
            mut.add("one" + i, "name", "emil", "age", 9, "full name", "Lienemann");
        }
        mut.update("one13", "age", 10);
        mut.remove("one13");
        System.out.println("add " + (System.currentTimeMillis() - tim));

        tim = System.currentTimeMillis();
        int count[] = {0};
        source.getStore().forEach( rec -> {
            count[0]++;
        });
        System.out.println("iter " + (System.currentTimeMillis() - tim)+" "+count[0]);
    }

    @Test
    public void test() {
        StorageDriver source = new StorageDriver(new HeapRecordStorage<>());
        FilterProcessor<String,Record<String>> stream = new FilterProcessor(source.getStore());
        source.setListener(stream);

        stream.subscribe(
            record -> "one".equals(record.getKey()),
            change -> System.out.println("listener: " + change)
        );

        Mutation mut = source;
        mut.add("one", "name", "emil", "age", 9);
        mut.add("two", "name", "felix", "age", 17);
        mut.update("one", "age", 10);
        mut.remove("one");

        source.getStore().forEach( rec -> {
            System.out.println(rec);
        });
    }

    @Test
    public void bench() {
        long tim = System.currentTimeMillis();
        for ( int ii = 0; ii < 100; ii++) {
            ChangeRequestBuilder cb = ChangeRequestBuilder.get();
            StorageDriver stream = new StorageDriver(new HeapRecordStorage<>());
            stream.setListener(change -> {
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
