package newimpl;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.impl.actors.RealLiveTableActor;
import org.nustaq.reallive.impl.actors.ShardedTable;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.*;
import org.nustaq.reallive.impl.storage.*;
import org.nustaq.reallive.records.MapRecord;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 04.08.2015.
 */
public class Basic {

    @Test
    public void testTableSpace() {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init();

        ts.createOrLoadTable(new TableDescription("blogs").numEntries(500_000).storageType(TableDescription.StorageType.TEMP).sizeMB(500)).await();
        ts.createOrLoadTable(new TableDescription("articles").numEntries(500_000 * 10).storageType(TableDescription.StorageType.TEMP).sizeMB(500 * 10)).await();

        RealLiveTable blogs = ts.getTableAsync("blogs").await();
        RealLiveTable articles = ts.getTableAsync("articles").await();

        int numBP = 30_000;
        for ( int i = 0; i < numBP; i++ ) {
            MapRecord blogEntry = MapRecord.New("blog"+i);
            blogEntry
                .put("title", "Java is beautiful")
                .put("description", "High performance Java, realtime distributed computing, other stuff")
                .put("tags", "#tech #java #distributed #actors #concurrency #webcomponents #realtime")
                .put("author", "R.Moeller")
                .put("likes", 199)
                .put("reads", 3485)
                .put("sequence", i );
            blogs.addRecord( blogEntry);
        }

        System.out.println("finished blogs");

        for ( int i = 0; i < numBP *10; i++ ) {
            MapRecord article = MapRecord.New("art"+i);
//            article
//                .putRecord("title", "WebComponents BLabla")
//                .putRecord("description", "High performance Java, realtime distributed computing, other stuff")
//                .putRecord("tags", "#tech #java #distributed #actors #concurrency #webcomponents #realtime")
//                .putRecord("author", "R.Moeller")
//                .putRecord("likes", 199)
//                .putRecord("reads", 3485)
//                .putRecord("blog", "blog"+i)
//                .putRecord("date", new Date() );
            article
                .put("tags", new String[] {"#tech", "#java2", "#distributed", "#actors", "#concurrency", "#webcomponents", "#realtime"})
                .put("likes", 199)
                .put("reads", 3485)
                .put("blog", "blog"+i)
                .put("date", System.currentTimeMillis() );
            articles.addRecord( article);
        }

        System.out.println("finished articles");

//        while( true )
        {
            long tim = System.currentTimeMillis();

            AtomicInteger counter = new AtomicInteger(0);

            blogs.forEachWithSpore(
                new FilterSpore(record -> record.getKey().indexOf("99") >= 0).setForEach((r, e) -> {

                })
            );
            blogs.ping().await();
            System.out.println("time blo " + (System.currentTimeMillis() - tim));
            System.out.println("hits:" + counter.get());
            counter.set(0);

            tim = System.currentTimeMillis();
            articles.forEachWithSpore(
                new FilterSpore(record -> record.getKey().indexOf("999") >= 0).setForEach((r, e) -> {

                })
            );
            articles.ping().await();
            System.out.println("time art " + (System.currentTimeMillis() - tim));
            System.out.println("hits:"+counter.get());
        }
        //ts.shutDown();
    }

//    @Test
//    public void testOffHeap() {
//        StorageDriver source = new StorageDriver(new OffHeapRecordStorage(32,500,600_000));
//        insertTest(source);
//    }

//    public void insertTest(StorageDriver source) {
//        FilterProcessorImpl<String> stream = new FilterProcessorImpl(source.getStore());
//        source.setListener(stream);
//
//        stream.subscribe(new Subscriber<>( null,
//                                             record -> "one13".equals(record.getKey()),
//                                             change -> System.out.println("listener: " + change)
//        ));
//
//        Mutation mut = source;
//        long tim = System.currentTimeMillis();
//        for ( int i = 0; i<500_000;i++ ) {
//            mut.add("one" + i, "name", "emil", "age", 9, "full name", "Lienemann");
//        }
//        mut.update("one13", "age", 10);
//        mut.remove("one13");
//        System.out.println("add " + (System.currentTimeMillis() - tim));
//
//        tim = System.currentTimeMillis();
//        int count[] = {0};
//        source.getStore().filter(rec -> true, (r, e) -> {
//            if ( Actors.isResult(e) )
//                count[0]++;
//        });
//        System.out.println("iter " + (System.currentTimeMillis() - tim)+" "+count[0]);
//    }

//    @Test
//    public void test() {
//        StorageDriver source = new StorageDriver(new HeapRecordStorage<>());
//        FilterProcessorImpl<String> stream = new FilterProcessorImpl(source.getStore());
//        source.setListener(stream);
//
//        stream.subscribe(new Subscriber<>( null,
//                                             record -> "one".equals(record.getKey()),
//                                             change -> System.out.println("listener: " + change)
//        ));
//
//        Mutation mut = source;
//        mut.add("one", "name", "emil", "age", 9);
//        mut.add("two", "name", "felix", "age", 17);
//        mut.add("one1", "name", "emil", "age", 9);
//        mut.add("two1", "name", "felix", "age", 17);
//        mut.update("one", "age", 10);
//        mut.remove("one");
//
//        source.getStore().filter(rec -> true, (r, e) -> System.out.println("REC:" + r));
//
//        source.getStore().filter( rec -> ((Record)rec).getInt("age") <= 10, (r, e) -> System.out.println("LQ:" + r));
//
//        try {
//            source.getStore().query("age <= 10 || age==17", (r, e) -> System.out.println("QUERY:" + r));
//        } catch (ParseException e) {
//            e.printStackTrace();
//        }
//    }
//
    @Test
    public void bench() {
        long tim = System.currentTimeMillis();
        for ( int ii = 0; ii < 100; ii++) {
            RLUtil cb = RLUtil.get();
            StorageDriver stream = new StorageDriver(new HeapRecordStorage());
            stream.setListener(change -> {
                //System.out.println(change);
            });
            tim = System.currentTimeMillis();
            for ( int i = 0; i < 100_000; i++ ) {
                stream.receive(cb.add( 1,"one"+i,
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
                stream.receive(cb.update(1,"one" + i, "age", 10));
            }
            System.out.println("UPD "+(System.currentTimeMillis()-tim) );
            tim = System.currentTimeMillis();
            for ( int i = 0; i < 100_000; i++ ) {
                stream.receive(cb.remove(1,"one"+i) );
            }
            System.out.println("DEL "+(System.currentTimeMillis()-tim) );
        }
    }

    public static class TA extends Actor<TA> {


        public IPromise randomTest(RealLiveTable rls) throws InterruptedException {
            HeapRecordStorage hstore = new HeapRecordStorage();
            StorageDriver copy = new StorageDriver(hstore);
            rls.subscribe(new Subscriber(r -> true,copy));

            for ( int i = 0; i < 1_000_000; i++ ) {
                double rand = Math.random() * 10;
                yield();
                rls.add( "k" + i,
                        "name", "rm",
                        "age", rand,
                        "arr", new int[]{1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5, 1, 2, 3, 4, 5,}
                );
            }
            rls.ping().await();
            System.out.println("ADD SIZE" + rls.size().await());

            for ( int i = 0; i < 1_000_000; i++ ) {
                double rand = Math.random() * 10;
                yield();
                rls.update( "k" + i,
                   "name", "rm"+(int)(Math.random()*i),
                   "age", rand,
                   "arr", new int[]{ 3, 4, 5, 1, 2, 3, 4, (int) rand}
                );
            }
            rls.ping().await();
            System.out.println("UPDATE SIZE" + rls.size().await());

            for ( int i = 0; i < 500_000; i++ ) {
                yield();
                rls.remove( "k" + (int) (Math.random() * 1_000_000));
            }
            rls.ping().await();
            System.out.println("REM SIZE" + rls.size().await());

            for ( int i = 0; i < 1_000_000; i++ ) {
                double rand = Math.random() * 10;
                yield();
                rls.merge( "k" + i,
                        "name", "rm",
                        "age", rand
                        );
            }
            rls.ping().await();
            System.out.println("ADD SIZE" + rls.size().await());

            System.out.println("comparing ..");
            return compare(hstore.getMap(), rls);
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
                rl.get((String) next.getKey()).then(rlRec -> {
                    if (pl.isComplete())
                        return;
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

        public IPromise runTest(RealLiveTable rls) throws InterruptedException {

            boolean inActor = Actor.inside();

            Subscriber subs =
                new Subscriber(
                    record -> "one13".equals(record.getKey()),
                    change -> {
                        if ( self() != null )
                            checkThread();
                        System.out.println("listener: " + change);
                    }
                );
            rls.subscribe(subs);


            long tim = 0;

            IntStream.range(0, 1).forEach(ii -> {
                if ( inActor )
                    checkThread();
                long tim1 = System.currentTimeMillis();
                int i1 = 500_000;
//                int i1 = 50;
                for (int i = 0; i < i1; i++) {
                    rls.merge( "one" + i, "name", "emil", "age", 9, "full name", "Lienemann");
                }
                rls.update( "one13", "age", 10);
                rls.remove( "one13");
                rls.ping().await();
                System.out.println("add " + (System.currentTimeMillis() - tim1));
            });

            Promise res = new Promise();
            tim = System.currentTimeMillis();
            int count[] = {0};
            final long finalTim = tim;
            rls.forEach(rec -> true, (r, e) -> {
                if ( inActor )
                    checkThread();
                if (isResult(e)) {
                    count[0]++;
//                    System.out.println(count[0]);
                }
                else {
                    System.out.println("count:"+count[0]+" tim:"+(System.currentTimeMillis()-finalTim));
                    res.resolve();
                }
            });

            return res;
        }
    }

    @Test
    public void testActor() throws InterruptedException {
        RealLiveTableActor rls = Actors.AsActor(RealLiveTableActor.class,100_000);
        rls.init( () -> new OffHeapRecordStorage(32, 500, 500_000), null);

        TA ta = Actors.AsActor(TA.class);
        ta.runTest(rls).await(20_000);
        ta.stop();
        rls.stop();
    }

    @Test
    public void testActorShard() throws InterruptedException {
        RealLiveTableActor rls[] = new RealLiveTableActor[8];
        for (int i = 0; i < rls.length; i++) {
            rls[i] = Actors.AsActor(RealLiveTableActor.class);
            rls[i].init(() -> new OffHeapRecordStorage(32, 500 / rls.length, 700_000 / rls.length), null);
        }
        ShardedTable sharding = new ShardedTable(rls, null);

        TA ta = Actors.AsActor(TA.class);
//        while( System.currentTimeMillis() != 0)
        {
            ta.runTest(sharding).await(50009);
        }
        ta.stop();
        sharding.stop();
    }

    @Test
    public void randomTestActorShard() throws InterruptedException {
//        while( System.currentTimeMillis() != 0)
        {
            RealLiveTableActor rls[] = new RealLiveTableActor[8];
            for (int i = 0; i < rls.length; i++) {
                rls[i] = Actors.AsActor(RealLiveTableActor.class);
                rls[i].init( () -> new OffHeapRecordStorage(32, 1500/rls.length, 1_500_000/rls.length), null);
            }
            ShardedTable sharding = new ShardedTable(rls, null);

            TA ta = Actors.AsActor(TA.class);
            ta.randomTest(sharding).await(500000);
            ta.stop();
            sharding.stop();
        }
    }

    @Test
    public void testActorOutside() throws InterruptedException {
        RealLiveTableActor rls = Actors.AsActor(RealLiveTableActor.class);
        rls.init(() -> new OffHeapRecordStorage(32, 500,500_000),null);

        TA ta = new TA();
        ta.runTest(rls).await();
        ta.stop();
        rls.stop();
    }

}
