package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.remoting.encoding.CallbackRefSerializer;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.*;
import org.nustaq.reallive.impl.storage.StorageStats;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.AddMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.function.*;

/**
 * Created by ruedi on 06.08.2015.
 *
 * core implementation of a table
 *
 * FIXME: missing
 * - CAS/updateActions
 * - originator
 *
 *
 */
public class RealLiveTableActor<K> extends Actor<RealLiveTableActor<K>> implements RealLiveTable<K>, Mutatable<K> {

    public static boolean DUMP_QUERY_TIME = false;

    StorageDriver<K> storageDriver;
    FilterProcessor<K> filterProcessor;
    HashMap<String,Subscriber> receiverSideSubsMap = new HashMap();
    TableDescription description;
    ArrayList<QueryQEntry> queuedSpores = new ArrayList();

    int taCount = 0;

    @Local
    public void init( Supplier<RecordStorage<K>> storeFactory, TableDescription desc) {
        this.description = desc;
        Thread.currentThread().setName("Table "+desc.getName()+" main");
        RecordStorage<K> store = storeFactory.get();
        storageDriver = new StorageDriver<>(store);
        filterProcessor = new FilterProcessor<>(this);
        storageDriver.setListener( filterProcessor );
    }


    @Override
    public void receive(ChangeMessage<K> change) {
        checkThread();
        try {
            storageDriver.receive(change);
        } catch (Exception th) {
            Log.Error(this,th);
        }
    }

    public <T> void forEachDirect(Spore<Record<K>, T> spore) {
        checkThread();
        try {
            storageDriver.getStore().forEach(spore);
        } catch (Exception ex) {
            spore.complete(null,ex);
        }
    }

    @Override
    public <T> void forEach(Spore<Record<K>, T> spore) {
        checkThread();
        try {
            Consumer<Record<K>> recordConsumer = rec -> {
                if (!spore.isFinished()) {
                    try {
                        spore.remote(rec);
                    } catch (Throwable ex) {
                        spore.complete(null, ex);
                    }
                }
            };
            if ( description.filterThreads() > 0 ) {
                storageDriver.getStore().stream().parallel().forEach(recordConsumer);
                spore.finish();
            } else {
                storageDriver.getStore().stream().forEach(recordConsumer);
                spore.finish();
            }
        } catch (Throwable ex) {
            spore.complete(null,ex);
        }
    }

    @Override
    protected void hasStopped() {
    }

    // subscribe/unsubscribe
    // on callerside, the subscription is decomposed to kontraktor primitives
    // and a subscription id (locally unique)
    // remote receiver then builds a unique id by concatening localid#connectionId

    @Override
    @CallerSideMethod public void subscribe(Subscriber<K> subs) {
        // need callerside to transform to Callback
        Callback callback = (r, e) -> {
            if (Actors.isResult(e))
                subs.getReceiver().receive((ChangeMessage<K>) r);
        };
        _subscribe(subs.getPrePatchFilter(),subs.getFilter(), callback, subs.getId());
    }

    public void _subscribe(RLPredicate<Record<K>> prePatchFilter, RLPredicate pred, Callback cb, int id) {
        checkThread();
        Subscriber localSubs = new Subscriber(prePatchFilter,pred, change -> {
            cb.stream(change);
        }).serverSideCB(cb);
        String sid = addChannelIdIfPresent(cb, ""+id);
        receiverSideSubsMap.put(sid,localSubs);

        FilterSpore spore = new FilterSpore(localSubs.getFilter(),localSubs.getPrePatchFilter());
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
    void forEachQueued( Spore s, Runnable r ) {
        queuedSpores.add(new QueryQEntry(s,r));
        self().execQueriesOrDelay(queuedSpores.size(),taCount);
    }

    public void execQueriesOrDelay(int size, int taCount) {
        if ( (queuedSpores.size() == size && this.taCount == taCount) || queuedSpores.size() > 10 ) {
            long tim = System.currentTimeMillis();
            Consumer<Record<K>> recordConsumer = rec -> {
                queuedSpores.forEach( qqentry -> {
                    Spore spore = qqentry.spore;
                    if (!spore.isFinished()) {
                        try {
                            spore.remote(rec);
                        } catch (Throwable ex) {
                            spore.complete(null, ex);
                        }
                    }
                });
            };
            if ( description.filterThreads() > 0 ) {
                storageDriver.getStore().stream().parallel().forEach(recordConsumer);
            } else {
                storageDriver.getStore().stream().forEach(recordConsumer);
            }
            queuedSpores.forEach( qqentry -> {
                qqentry.spore.finish();
                qqentry.onFin.run();
            });
            if (DUMP_QUERY_TIME)
                System.out.println("tim for "+queuedSpores.size()+" "+(System.currentTimeMillis()-tim));
            queuedSpores.clear();
            return;
        }
        execQueriesOrDelay(queuedSpores.size(),this.taCount);
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
    public void unsubscribe(Subscriber<K> subs) {
        _unsubscribe( (r,e) -> {}, subs.getId() );
    }

    public void _unsubscribe( Callback cb /*dummy required to find sending connection*/, int id ) {
        checkThread();
        String sid = addChannelIdIfPresent(cb, ""+id);
        Subscriber<K> subs = (Subscriber<K>) receiverSideSubsMap.get(sid);
        filterProcessor.unsubscribe(subs);
        receiverSideSubsMap.remove(sid);
        cb.finish();
        subs.getServerSideCB().finish();
    }

    @Override
    public IPromise<Record<K>> get(K key) {
        return resolve(storageDriver.getStore().get(key));
    }

    @Override
    public IPromise<Long> size() {
        return resolve(storageDriver.getStore().size());
    }

    @Override @CallerSideMethod
    public Mutation<K> getMutation() {
        return new Mutator<>(self());
    }

    @Override
    public IPromise<TableDescription> getDescription() {
        return resolve(description);
    }

    @Override
    public IPromise<StorageStats> getStats() {
        return resolve(storageDriver.getStore().getStats());
    }

    @Override
    public IPromise<Boolean> putCAS(RLPredicate<Record<K>> casCondition, K key, Object[] keyVals) {
        return storageDriver.putCAS(casCondition,key,keyVals);
    }

    @Override
    public void atomic(K key, RLConsumer<Record<K>> action) {
        storageDriver.atomic(key,action);
    }

}
