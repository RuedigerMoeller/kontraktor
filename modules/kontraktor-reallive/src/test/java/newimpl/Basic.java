package newimpl;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.actors.RealLiveStreamActor;
import org.nustaq.reallive.actors.ShardFunc;
import org.nustaq.reallive.actors.Sharding;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.*;
import org.nustaq.reallive.records.MapRecord;
import org.nustaq.reallive.storage.*;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 04.08.2015.
 */
public class Basic {

    @Test
    public void testOffHeap() {
        StorageDriver source = new StorageDriver(new OffHeapRecordStorage<>(32,500,600_000));
        insertTest(source);
    }

    public void insertTest(StorageDriver source) {
        FilterProcessor<String,Record<String>> stream = new FilterProcessor(source.getStore());
        source.setListener(stream);

        stream.subscribe( new Subscriber<>(
            record -> "one13".equals(record.getKey()),
            change -> System.out.println("listener: " + change)
        ));

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
        source.getStore().forEach( rec -> true, rec -> {
            count[0]++;
        });
        System.out.println("iter " + (System.currentTimeMillis() - tim)+" "+count[0]);
    }

    @Test
    public void test() {
        StorageDriver source = new StorageDriver(new HeapRecordStorage<>());
        FilterProcessor<String,Record<String>> stream = new FilterProcessor(source.getStore());
        source.setListener(stream);

        stream.subscribe(new Subscriber<>(
            record -> "one".equals(record.getKey()),
            change -> System.out.println("listener: " + change)
        ));

        Mutation mut = source;
        mut.add("one", "name", "emil", "age", 9);
        mut.add("two", "name", "felix", "age", 17);
        mut.update("one", "age", 10);
        mut.remove("one");

        source.getStore().forEach( rec -> true, rec -> {
            System.out.println(rec);
        });
    }

    @Test
    public void bench() {
        long tim = System.currentTimeMillis();
        for ( int ii = 0; ii < 100; ii++) {
            RLUtil cb = RLUtil.get();
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

    public static class TA extends Actor<TA> {


        public IPromise randomTest(RealLiveTable<String, Record<String>> rls) throws InterruptedException {
            HashMap<String, Record<String>> copy = new HashMap<>();

            for ( int i = 0; i < 1_000_000; i++ ) {
                double rand = Math.random() * 10;
                rls.add("k" + i,
                           "name", "rm",
                           "age", rand,
                           "arr", new int[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5,}
                );
                MapRecord<String> mrec
                    = new MapRecord<String>(
                        "k" + i,
                        "name", "rm",
                        "age", rand,
                        "arr", new int[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5}
                    );
                copy.put(mrec.getKey(), mrec);
            }
            rls.ping().await();
            System.out.println("SIZE" + rls.size().await());
            System.out.println("comparing ..");
            return compare(copy, rls);
        }

        IPromise compare( Map m, RealLiveTable rl ) {
            PromiseLatch pl = new PromiseLatch(m.size());
            AtomicInteger count = new AtomicInteger(0);
            for (Iterator<Map.Entry> iterator = m.entrySet().iterator(); iterator.hasNext(); ) {
                Map.Entry next = iterator.next();
                Record copy = (Record) next.getValue();
                yield();
                if ( count.incrementAndGet() % 100_000 == 0 )
                    System.out.println("compared "+count.get());
                rl.get(next.getKey()).then(rlRec -> {
                    checkThread();
                    try {
                        if ( !RLUtil.get().isEqual( (Record) rlRec, copy) ) {
                            pl.reject("ERR");
                        } else {
                            pl.countDown();
                        }
                    } catch (Exception e) {
//                        e.printStackTrace();
                        pl.reject("ERR");
//                        System.exit(-1);
                    }
                });
            }
            return pl.getPromise();
        }

        public IPromise runTest(RealLiveTable<String, Record<String>> rls) throws InterruptedException {

            Subscriber<String, Record<String>> subs =
                new Subscriber<>(
                    record -> "one13".equals(record.getKey()),
                    change -> {
                    if ( self() != null )
                        checkThread();
                      System.out.println("listener: " + change);
                    }
                );
            rls.subscribe(subs);

            Mutation mut = rls;

            long tim = 0;
            IntStream.range(0, 1).forEach(ii -> {
                long tim1 = System.currentTimeMillis();
                for (int i = 0; i < 500_000; i++) {
                    mut.put("one" + i, "name", "emil", "age", 9, "full name", "Lienemann");
                }
                mut.update("one13", "age", 10);
                mut.remove("one13");
                rls.ping().await();
                System.out.println("add " + (System.currentTimeMillis() - tim1));
            });

            Promise res = new Promise();
            tim = System.currentTimeMillis();
            int count[] = {0};
            final long finalTim = tim;
            rls.forEach(rec -> true, rec -> {
                if ( self() != null )
                    checkThread();
                count[0]++;
                if (count[0] == 500_000-1) {
                    System.out.println("iter " + (System.currentTimeMillis() - finalTim) + " " + count[0]);
                    res.complete();
                    rls.unsubscribe(subs);
                }
            });

            return res;
        }
    }

    @Test
    public void testActor() throws InterruptedException {
        RealLiveStreamActor<String,Record<String>> rls = Actors.AsActor(RealLiveStreamActor.class);
        rls.init( () -> new OffHeapRecordStorage<>(32,500,500_000),true);

        TA ta = Actors.AsActor(TA.class);
        ta.runTest(rls).await();
        ta.stop();
        rls.stop();
    }

    @Test
    public void testActorShard() throws InterruptedException {
        RealLiveStreamActor<String,Record<String>> rls[] = new RealLiveStreamActor[8];
        for (int i = 0; i < rls.length; i++) {
            rls[i] = Actors.AsActor(RealLiveStreamActor.class);
            rls[i].init( () -> new OffHeapRecordStorage<>(32, 500/rls.length, 500_000/rls.length), false);
        }
        ShardFunc<String> sfunc = key -> Math.abs(key.hashCode()) % rls.length;
        Sharding<String,Record<String>> sharding = new Sharding<>(sfunc, rls);

        TA ta = Actors.AsActor(TA.class);
        while( System.currentTimeMillis() != 0) {
            ta.runTest(sharding).await(500000);
        }
        ta.stop();
        sharding.stop();
    }

    @Test
    public void randomTestActorShard() throws InterruptedException {
        while( System.currentTimeMillis() != 0)
        {
            RealLiveStreamActor<String,Record<String>> rls[] = new RealLiveStreamActor[8];
            for (int i = 0; i < rls.length; i++) {
                rls[i] = Actors.AsActor(RealLiveStreamActor.class);
                rls[i].init( () -> new OffHeapRecordStorage<>(32, 1500/rls.length, 1_500_000/rls.length), false);
            }
            ShardFunc<String> sfunc = key -> Math.abs(key.hashCode()) % rls.length;
            Sharding<String,Record<String>> sharding = new Sharding<>(sfunc, rls);

            TA ta = Actors.AsActor(TA.class);
                ta.randomTest(sharding).await(500000);
            ta.stop();
            sharding.stop();
        }
    }

    @Test
    public void testActorOutside() throws InterruptedException {
        RealLiveStreamActor<String,Record<String>> rls = Actors.AsActor(RealLiveStreamActor.class);
        rls.init(() -> new OffHeapRecordStorage<>(32, 500,500_000),true);

        TA ta = new TA();
        ta.runTest(rls).await();
        ta.stop();
        rls.stop();
    }


}
