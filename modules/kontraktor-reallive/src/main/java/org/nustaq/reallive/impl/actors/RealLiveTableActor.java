package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.remoting.encoding.CallbackRefSerializer;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.*;
import org.nustaq.reallive.impl.storage.StorageStats;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.*;

/**
 * Created by ruedi on 06.08.2015.
 *
 * core implementation of a table
 *
 */
public class RealLiveTableActor extends Actor<RealLiveTableActor> implements RealLiveTable {

    public static int MAX_QUERY_BATCH_SIZE = 10;
    public static boolean DUMP_QUERY_TIME = false;

    StorageDriver storageDriver;
    FilterProcessor filterProcessor;
    HashMap<String,Subscriber> receiverSideSubsMap = new HashMap();
    TableDescription description;
    ArrayList<QueryQEntry> queuedSpores = new ArrayList();

    int taCount = 0;

    @Local
    public IPromise init(Supplier<RecordStorage> storeFactory, TableDescription desc) {
        this.description = desc;
        Thread.currentThread().setName("Table "+(desc==null?"NULL":desc.getName())+" main");
        RecordStorage store = storeFactory.get();
        storageDriver = new StorageDriver(store);
        filterProcessor = new FilterProcessor(this);
        storageDriver.setListener( filterProcessor );
        return resolve();
    }

    @Override
    public void receive(ChangeMessage change) {
        checkThread();
        try {
            storageDriver.receive(change);
        } catch (Exception th) {
            Log.Error(this,th);
        }
    }

    public <T> void forEachDirect(Spore<Record, T> spore) {
        checkThread();
        try {
            storageDriver.getStore().forEachWithSpore(spore);
        } catch (Exception ex) {
            spore.complete(null,ex);
        }
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        forEachQueued(spore, () -> {});
    }

    @Override
    protected void hasStopped() {
    }

    // subscribe/unsubscribe
    // on callerside, the subscription is decomposed to kontraktor primitives
    // and a subscription id (locally unique)
    // remote receiver then builds a unique id by concatening localid#connectionId

    @Override
    @CallerSideMethod public void subscribe(Subscriber subs) {
        // need callerside to transform to Callback
        Callback callback = (r, e) -> {
            if (Actors.isResult(e))
                subs.getReceiver().receive((ChangeMessage) r);
        };
        _subscribe(subs.getFilter(), callback, subs.getId());
    }

    public void _subscribe(RLPredicate pred, Callback cb, int id) {
        checkThread();
        Subscriber localSubs = new Subscriber(pred, change -> {
            cb.pipe(change);
        }).serverSideCB(cb);
        String sid = addChannelIdIfPresent(cb, ""+id);
        receiverSideSubsMap.put(sid,localSubs);

        FilterSpore spore = new FilterSpore(localSubs.getFilter()).modifiesResult(false);
        spore.onFinish( () -> localSubs.getReceiver().receive(RLUtil.get().done()) );
        spore.setForEach((r, e) -> {
            if (Actors.isResult(e)) {
                localSubs.getReceiver().receive(new AddMessage((Record) r));
            } else {
                // FIXME: pass errors
                // FIXME: called in case of error only (see onFinish above)
                localSubs.getReceiver().receive(RLUtil.get().done());
            }
        });
        forEachQueued(spore, () -> {
            filterProcessor.startListening(localSubs);
        });
    }

    static class QueryQEntry {
        Spore spore;
        Runnable onFin;

        public QueryQEntry(Spore spore, Runnable onFin) {
            this.spore = spore;
            this.onFin = onFin;
        }
    }

    private void forEachQueued( Spore s, Runnable r ) {
        queuedSpores.add(new QueryQEntry(s,r));
        self()._execQueriesOrDelay(queuedSpores.size(),taCount);
    }

    public void _execQueriesOrDelay(int size, int taCount) {
        if ( (queuedSpores.size() == size && this.taCount == taCount) || queuedSpores.size() > MAX_QUERY_BATCH_SIZE) {
            long tim = System.currentTimeMillis();
            Consumer<Record> recordConsumer = rec -> {
                for (int i = 0; i < queuedSpores.size(); i++) {
                    QueryQEntry qqentry = queuedSpores.get(i);
                    Spore spore = qqentry.spore;
                    if (!spore.isFinished()) {
                        try {
                            spore.remote(rec);
                        } catch (Throwable ex) {
                            spore.complete(null, ex);
                        }
                    }
                }
            };
            storageDriver.getStore().stream().forEach(recordConsumer);
            queuedSpores.forEach( qqentry -> {
                qqentry.spore.finish();
                qqentry.onFin.run();
            });
            if (DUMP_QUERY_TIME)
                System.out.println("tim for "+queuedSpores.size()+" "+(System.currentTimeMillis()-tim));
            queuedSpores.clear();
            return;
        }
        _execQueriesOrDelay(queuedSpores.size(),this.taCount);
    }

    protected String addChannelIdIfPresent(Callback cb, String sid) {
        if ( cb instanceof CallbackWrapper && ((CallbackWrapper) cb).isRemote() ) {
            // hack to get unique id sender#connection
            CallbackRefSerializer.MyRemotedCallback realCallback
                = (CallbackRefSerializer.MyRemotedCallback) ((CallbackWrapper) cb).getRealCallback();
            sid += "#"+realCallback.getChanId();
        }
        return sid;
    }

    @CallerSideMethod @Override
    public void unsubscribe(Subscriber subs) {
        _unsubscribe( (r,e) -> {}, subs.getId() );
    }

    public void _unsubscribe( Callback cb /*dummy required to find sending connection*/, int id ) {
        checkThread();
        String sid = addChannelIdIfPresent(cb, ""+id);
        Subscriber subs = (Subscriber) receiverSideSubsMap.get(sid);
        filterProcessor.unsubscribe(subs);
        receiverSideSubsMap.remove(sid);
        cb.finish();
        subs.getServerSideCB().finish();
    }

    @Override
    public IPromise<Record> get(String key) {
        taCount++;
        return resolve(storageDriver.getStore().get(key));
    }

    @Override
    public IPromise<Long> size() {
        return resolve(storageDriver.getStore().size());
    }

    @Override
    public IPromise<TableDescription> getDescription() {
        return resolve(description);
    }

    @Override
    public IPromise<StorageStats> getStats() {
        try {
            final StorageStats stats = storageDriver.getStore().getStats();
            return resolve(stats);
        } catch (Throwable th) {
            Log.Warn(this,th);
            return reject(th.getMessage());
        }
    }

    @Override
    public IPromise atomic(String key, RLFunction<Record, Object> action) {
        taCount++;
        return storageDriver.atomicQuery(key,action);
    }

    @Override
    public void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        taCount++;
        storageDriver.atomicUpdate(filter, action);
    }

    @Override
    public IPromise resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        Log.Info(this,"resizing table if lf: "+loadFactor+" maxgrow:"+maxGrowBytes);
        long now = System.currentTimeMillis();
        storageDriver.resizeIfLoadFactorLarger(loadFactor,maxGrowBytes);
        Log.Info(this,"resizing duration"+(System.currentTimeMillis()-now));
        return resolve();
    }

    @Override
    public void put(String key, Object ... keyVals) {
        receive(RLUtil.get().put(key, keyVals));
    }

    @Override
    public void merge(String key, Object... keyVals) {
        if ( ((Object)key) instanceof Record )
            throw new RuntimeException("probably accidental method resolution fail. Use merge instead");
        receive(RLUtil.get().addOrUpdate(key, keyVals));
    }

    @Override
    public IPromise<Boolean> add(String key, Object... keyVals) {
        if ( storageDriver.getStore().get(key) != null )
            return resolve(false);
        receive(RLUtil.get().add(key, keyVals));
        return resolve(true);
    }

    @Override
    public IPromise<Boolean> addRecord(Record rec) {
        if ( rec instanceof RecordWrapper)
            rec = ((RecordWrapper) rec).getRecord();
        if ( storageDriver.getStore().get(rec.getKey()) != null )
            return resolve(false);
        receive((ChangeMessage) new AddMessage(rec));
        return resolve(true);
    }

    @Override
    public void mergeRecord(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receive(new AddMessage(true,rec));
    }

    @Override
    public void setRecord(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receive(new PutMessage(rec));
    }

    @Override
    public void update(String key, Object... keyVals) {
        receive(RLUtil.get().update(key, keyVals));
    }

    @Override
    public IPromise<Record> take(String key) {
        Record record = storageDriver.getStore().get(key);
        receive(RLUtil.get().remove(key));
        return resolve(record);
    }

    @Override
    public void remove(String key) {
        RemoveMessage remove = RLUtil.get().remove(key);
        receive(remove);
    }

}
