package newimpl;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.impl.actors.RealLiveStreamActor;
import org.nustaq.reallive.impl.actors.ShardFunc;
import org.nustaq.reallive.impl.actors.TableSharding;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.*;
import org.nustaq.reallive.impl.storage.*;
import org.nustaq.reallive.records.MapRecord;

import java.text.ParseException;
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
        ts.init(4, 0);

        ts.createOrLoadTable(new TableDescription("blogs").numEntries(500_000).sizeMB(500));
        ts.createOrLoadTable(new TableDescription("articles").numEntries(500_000 * 10).sizeMB(500 * 10));

        RealLiveTable<String> blogs = ts.getTable("blogs").await();
        Mutation<String> blogMutation = blogs.getMutation();

        RealLiveTable<String> articles = ts.getTable("articles").await();
        Mutation<String> artMutation = articles.getMutation();

        int numBP = 30_000;
        for ( int i = 0; i < numBP; i++ ) {
            MapRecord<String> blogEntry = new MapRecord<>("blog"+i);
            blogEntry
                .put("title", "Java is beautiful")
                .put("description", "High performance Java, realtime distributed computing, other stuff")
                .put("tags", "#tech #java #distributed #actors #concurrency #webcomponents #realtime")
                .put("author", "R.Moeller")
                .put("likes", 199)
                .put("reads", 3485)
                .put("sequence", i );
            blogMutation.add(blogEntry);
        }

        System.out.println("finished blogs");

        for ( int i = 0; i < numBP *10; i++ ) {
            MapRecord<String> article = new MapRecord<>("art"+i);
//            article
//                .put("title", "WebComponents BLabla")
//                .put("description", "High performance Java, realtime distributed computing, other stuff")
//                .put("tags", "#tech #java #distributed #actors #concurrency #webcomponents #realtime")
//                .put("author", "R.Moeller")
//                .put("likes", 199)
//                .put("reads", 3485)
//                .put("blog", "blog"+i)
//                .put("date", new Date() );
            article
                .put("tags", new String[] {"#tech", "#java2", "#distributed", "#actors", "#concurrency", "#webcomponents", "#realtime"})
                .put("likes", 199)
                .put("reads", 3485)
                .put("blog", "blog"+i)
                .put("date", System.currentTimeMillis() );
            artMutation.add(article);
        }

        System.out.println("finished articles");

        while( true ) {
            long tim = System.currentTimeMillis();

            AtomicInteger counter = new AtomicInteger(0);

            blogs.forEach(
                new FilterSpore<String>(record -> record.getKey().indexOf("99") >= 0).forEach((r, e) -> {

                })
            );
            blogs.ping().await();
            System.out.println("time blo " + (System.currentTimeMillis() - tim));
            System.out.println("hits:" + counter.get());
            counter.set(0);

            tim = System.currentTimeMillis();
            articles.forEach(
                new FilterSpore<String>(record -> record.getKey().indexOf("999") >= 0).forEach( (r,e) -> {

                })
            );
            articles.ping().await();
            System.out.println("time art " + (System.currentTimeMillis() - tim));
            System.out.println("hits:"+counter.get());
        }
        //ts.shutDown();
    }

    @Test
    public void testOffHeap() {
        StorageDriver source = new StorageDriver(new OffHeapRecordStorage(32,500,600_000));
        insertTest(source);
    }

    public void insertTest(StorageDriver source) {
        FilterProcessor<String> stream = new FilterProcessor(source.getStore());
        source.setListener(stream);

        stream.subscribe(new Subscriber<>(
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
        source.getStore().filter(rec -> true, (r, e) -> {
            if ( Actors.isResult(e) )
                count[0]++;
        });
        System.out.println("iter " + (System.currentTimeMillis() - tim)+" "+count[0]);
    }

    @Test
    public void test() {
        StorageDriver source = new StorageDriver(new HeapRecordStorage<>());
        FilterProcessor<String> stream = new FilterProcessor(source.getStore());
        source.setListener(stream);

        stream.subscribe(new Subscriber<>(
                                             record -> "one".equals(record.getKey()),
                                             change -> System.out.println("listener: " + change)
        ));

        Mutation mut = source;
        mut.add("one", "name", "emil", "age", 9);
        mut.add("two", "name", "felix", "age", 17);
        mut.add("one1", "name", "emil", "age", 9);
        mut.add("two1", "name", "felix", "age", 17);
        mut.update("one", "age", 10);
        mut.remove("one");

        source.getStore().filter(rec -> true, (r, e) -> System.out.println("REC:" + r));

        source.getStore().filter( rec -> ((Record)rec).getInt("age") <= 10, (r, e) -> System.out.println("LQ:" + r));

        try {
            source.getStore().query("age <= 10 || age==17", (r, e) -> System.out.println("QUERY:" + r));
        } catch (ParseException e) {
            e.printStackTrace();
        }
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


        public IPromise randomTest(RealLiveTable<String> rls) throws InterruptedException {
            HeapRecordStorage<Object> hstore = new HeapRecordStorage<>();
            StorageDriver<String> copy = new StorageDriver(hstore);
            rls.subscribe(new Subscriber<>(r -> true,copy));
            Mutation<String> mut = rls.getMutation();

            for ( int i = 0; i < 1_000_000; i++ ) {
                double rand = Math.random() * 10;
                yield();
                mut.add("k" + i,
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
                mut.update("k" + i,
                   "name", "rm"+(int)(Math.random()*i),
                   "age", rand,
                   "arr", new int[]{ 3, 4, 5, 1, 2, 3, 4, (int) rand}
                );
            }
            rls.ping().await();
            System.out.println("UPDATE SIZE" + rls.size().await());

            for ( int i = 0; i < 500_000; i++ ) {
                yield();
                mut.remove("k" + (int) (Math.random() * 1_000_000));
            }
            rls.ping().await();
            System.out.println("REM SIZE" + rls.size().await());

            for ( int i = 0; i < 1_000_000; i++ ) {
                double rand = Math.random() * 10;
                yield();
                mut.addOrUpdate("k" + i,
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
                rl.get(next.getKey()).then(rlRec -> {
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

        public IPromise runTest(RealLiveTable<String> rls) throws InterruptedException {

            boolean inActor = Actor.inside();

            Subscriber<String> subs =
                new Subscriber<>(
                    record -> "one13".equals(record.getKey()),
                    change -> {
                        if ( self() != null )
                            checkThread();
                        System.out.println("listener: " + change);
                    }
                );
            rls.subscribe(subs);

            Mutation mut = rls.getMutation();

            long tim = 0;

            IntStream.range(0, 1).forEach(ii -> {
                if ( inActor )
                    checkThread();
                long tim1 = System.currentTimeMillis();
                for (int i = 0; i < 500_000; i++) {
                    mut.addOrUpdate("one" + i, "name", "emil", "age", 9, "full name", "Lienemann");
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
            rls.filter(rec -> true, (r, e) -> {
                if ( inActor )
                    checkThread();
                if (isResult(e))
                    count[0]++;
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
        RealLiveStreamActor<String> rls = Actors.AsActor(RealLiveStreamActor.class,100_000);
        rls.init( () -> new OffHeapRecordStorage(32, 500, 500_000), new SimpleScheduler(), null);

        TA ta = Actors.AsActor(TA.class);
        ta.runTest(rls).await(20_000);
        ta.stop();
        rls.stop();
    }

    @Test
    public void testActorShard() throws InterruptedException {
        RealLiveStreamActor<String> rls[] = new RealLiveStreamActor[8];
        for (int i = 0; i < rls.length; i++) {
            rls[i] = Actors.AsActor(RealLiveStreamActor.class);
            rls[i].init(() -> new OffHeapRecordStorage(32, 500 / rls.length, 700_000 / rls.length), rls[i].getScheduler(), null);
        }
        ShardFunc<String> sfunc = key -> Math.abs(key.hashCode()) % rls.length;
        TableSharding<String> sharding = new TableSharding<>(sfunc, rls, null);

        TA ta = Actors.AsActor(TA.class);
        while( System.currentTimeMillis() != 0) {
            ta.runTest(sharding).await(500000);
        }
        ta.stop();
        sharding.stop();
    }

    @Test
    public void randomTestActorShard() throws InterruptedException {
//        while( System.currentTimeMillis() != 0)
        {
            RealLiveStreamActor<String> rls[] = new RealLiveStreamActor[8];
            for (int i = 0; i < rls.length; i++) {
                rls[i] = Actors.AsActor(RealLiveStreamActor.class);
                rls[i].init( () -> new OffHeapRecordStorage(32, 1500/rls.length, 1_500_000/rls.length), rls[i].getScheduler(), null);
            }
            ShardFunc<String> sfunc = key -> Math.abs(key.hashCode()) % rls.length;
            TableSharding<String> sharding = new TableSharding<>(sfunc, rls, null);

            TA ta = Actors.AsActor(TA.class);
            ta.randomTest(sharding).await(500000);
            ta.stop();
            sharding.stop();
        }
    }

    @Test
    public void testActorOutside() throws InterruptedException {
        RealLiveStreamActor<String> rls = Actors.AsActor(RealLiveStreamActor.class);
        rls.init(() -> new OffHeapRecordStorage(32, 500,500_000),new SimpleScheduler(),null);

        TA ta = new TA();
        ta.runTest(rls).await();
        ta.stop();
        rls.stop();
    }


}
