package org.nustaq.reallive.impl.tablespace;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import org.nustaq.reallive.impl.actors.RealLiveStreamActor;
import org.nustaq.reallive.impl.storage.OffHeapRecordStorage;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.StateMessage;
import org.nustaq.reallive.records.MapRecord;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

/**
 * Created by ruedi on 08.08.2015.
 */
public class TableSpaceActor extends Actor<TableSpaceActor> implements TableSpace {

    HashMap<String,RealLiveTable> tables;
    HashMap<String,TableDescription> tableDesc;
    List<Callback<StateMessage>> stateListeners;

    SimpleScheduler scanScheduler[];
    SimpleScheduler filterScheduler[];
    int scanQSize = 64000;
    int filterQSize = 64000;

    @Local
    public void init( int numScanThreads, int numFilterThreads ) {
        tables = new HashMap<>();
        stateListeners = new ArrayList<>();
        tableDesc = new HashMap<>();
        scanScheduler = new SimpleScheduler[numScanThreads];
        filterScheduler = new SimpleScheduler[numFilterThreads];
        for (int i = 0; i < scanScheduler.length; i++) {
            scanScheduler[i] = new SimpleScheduler(scanQSize,true);
        }
        for (int i = 0; i < filterScheduler.length; i++) {
            filterScheduler[i] = new SimpleScheduler(filterQSize,true);
        }
    }

    @Override
    public IPromise<RealLiveTable> createTable(TableDescription desc) {
        if ( tables.containsKey( desc.getName()) )
            reject("Table exists");
        RealLiveStreamActor table = Actors.AsActor(RealLiveStreamActor.class, loadBalance(desc));

        Supplier<RecordStorage> memFactory;
        if ( desc.getFilePath() == null ) {
            memFactory = () -> new OffHeapRecordStorage<>( 48, desc.getSizeMB(), desc.getNumEntries() );
        } else {
            memFactory = null;
        }
        table.init( memFactory, loadBalanceFilter(desc), desc );
        tables.put(desc.getName(),table);
        return resolve(table);
    }

    int roundRobinFilterCounter = 0;
    private Scheduler loadBalanceFilter(TableDescription desc) {
        if ( filterScheduler.length <= 0 )  // if no threads, just use a scan thread
            return loadBalance(desc);
        roundRobinFilterCounter++;
        if ( roundRobinFilterCounter >= filterScheduler.length )
            roundRobinFilterCounter = 0;
        return filterScheduler[roundRobinFilterCounter];
    }

    int roundRobinScanCounter = 0;
    private Scheduler loadBalance(TableDescription desc) {
        roundRobinScanCounter++;
        if ( roundRobinScanCounter >= scanScheduler.length )
            roundRobinScanCounter = 0;
        return scanScheduler[roundRobinScanCounter];
    }

    @Override
    public IPromise dropTable(String name) {
        RealLiveTable realLiveTable = tables.get(name);
        if ( name != null ) {
            tables.remove(name);
            ((Actor)realLiveTable).stop();
        }
        return resolve();
    }

    @Override
    public IPromise<List<TableDescription>> getTableDescriptions() {
        return resolve( new ArrayList<TableDescription>(tableDesc.values()) );
    }

    @Override
    public IPromise<List<RealLiveTable>> getTables() {
        return resolve( new ArrayList<>(tables.values()));
    }

    @Override
    public IPromise<RealLiveTable> getTable(String name) {
        return resolve(tables.get(name));
    }

    @Override
    public IPromise shutDown() {
        return null;
    }

    @Override
    public void stateListener(Callback<StateMessage> stateListener) {
        this.stateListeners.add(stateListener);
    }

    public static void main(String[] args) {
        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);
        ts.init(4, 0);

        ts.createTable(new TableDescription("blogs").numEntries(500_000).sizeMB(500));
        ts.createTable(new TableDescription("articles").numEntries(500_000*10).sizeMB(500*10));

        RealLiveTable<String, Record<String>> blogs = ts.getTable("blogs").await();
        Mutation<String, Record<String>> blogMutation = blogs.getMutation();

        RealLiveTable<String, Record<String>> articles = ts.getTable("articles").await();
        Mutation<String, Record<String>> artMutation = articles.getMutation();

        for ( int i = 0; i < 300_000; i++ ) {
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

        for ( int i = 0; i < 300_000*10; i++ ) {
            MapRecord<String> article = new MapRecord<>("art"+i);
            article
                .put("title", "WebComponents BLabla")
                .put("description", "High performance Java, realtime distributed computing, other stuff")
                .put("tags", "#tech #java #distributed #actors #concurrency #webcomponents #realtime")
                .put("author", "R.Moeller")
                .put("likes", 199)
                .put("reads", 3485)
                .put("blog", "blog"+i)
                .put("date", new Date() );
            artMutation.add(article);
        }

        System.out.println("finished articles");

        while( true ) {
            long tim = System.currentTimeMillis();

            AtomicInteger counter = new AtomicInteger(0);
            blogs.forEach( record -> record.getKey().indexOf("99") >= 0, result -> {
                counter.incrementAndGet();
            });
            blogs.ping().await();
            System.out.println("time blo " + (System.currentTimeMillis() - tim));
            System.out.println("hits:" + counter.get());
            counter.set(0);

            tim = System.currentTimeMillis();
            articles.forEach( record -> record.getKey().indexOf("999") >= 0, result -> {
                counter.incrementAndGet();
            });
            articles.ping().await();
            System.out.println("time art " + (System.currentTimeMillis() - tim));
            System.out.println("hits:"+counter.get());
        }

    }

}
