package org.nustaq.reallive.server.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.remoting.encoding.CallbackRefSerializer;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.server.*;
import org.nustaq.reallive.server.storage.ClusterTableRecordMapping;
import org.nustaq.reallive.server.storage.HashIndex;
import org.nustaq.reallive.server.storage.IndexedRecordStorage;
import org.nustaq.reallive.server.storage.StorageStats;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.query.QToken;
import org.nustaq.reallive.query.Value;
import org.nustaq.reallive.query.VarPath;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.*;
import java.util.stream.Stream;

import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 06.08.2015.
 *
 * core implementation of a table
 *
 */
public class RealLiveTableActor extends Actor<RealLiveTableActor> implements RealLiveTable {

    public static final long REPORT_INTERVAL = TimeUnit.MINUTES.toMillis(1);
    public static boolean DUMP_QUERY_TIME = true;

    public transient String __clientSideTag; // usually data shard name

    StorageDriver storageDriver;
    FilterProcessor filterProcessor;
    HashMap<String,Subscriber> receiverSideSubsMap = new HashMap();
    TableDescription description;
    IndexedRecordStorage indexedStorage = new IndexedRecordStorage(); // holds indizes
    ArrayList<QueryQEntry> queuedSpores = new ArrayList();
    ClusterTableRecordMapping mapping = new ClusterTableRecordMapping();

    int taCount = 0;
    long lastReportTime = System.currentTimeMillis();

    @Local
    public IPromise init(Function<TableDescription,RecordStorage> storeFactory, TableDescription desc) {
        this.description = desc;
        Thread.currentThread().setName("Table "+(desc==null?"NULL":desc.getName())+" main");
        RecordStorage store = storeFactory.apply(desc);
        indexedStorage.wrapped(store);
        createIndizes(store);
        filterProcessor = new FilterProcessor(this);
        storageDriver = new StorageDriver(indexedStorage);
        storageDriver.setListener( filterProcessor );
        return resolve();
    }

    private void createIndizes(RecordStorage rawStorage) {
        if ( description.getHashIndexed() != null && description.getHashIndexed().length > 0) {
            for (int i = 0; i < description.getHashIndexed().length; i++) {
                String path = description.getHashIndexed()[i];
                VarPath vp = new VarPath(path,null, new QToken(path,"",0 ));
                indexedStorage.addIndex( new HashIndex( rec -> {
                    Value evaluate = vp.evaluate(rec);
                    if ( evaluate == null )
                        return null;
                    return evaluate.getValue();
                },
                    path
                ));
            }
        }
        rawStorage.forEach( rec -> true, (r,e) -> {
            if ( r != null ) {
                indexedStorage.initializeFromRecord(r);
                mapping.setBucket( mapping.getBucket(r.getKey().hashCode()),true);
            }
        });
        Log.Info(this,"index creation done "+description.getName()+" "+description.getShardNo());
    }

    public IPromise<ClusterTableRecordMapping> getRecordMapping() {
        return resolve(mapping);
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
        long now = System.currentTimeMillis();
        if ( now-lastReportTime > REPORT_INTERVAL) {
            Log.Info(this,"mem report filterProc "+filterProcessor.getFilterSize()+", receiverSideSubsMap:"+receiverSideSubsMap.size() );
            lastReportTime = now;
        }
        Subscriber localSubs = new Subscriber(pred, change -> {
            cb.pipe(change);
        }).serverSideCB(cb);
        String sid = addChannelIdIfPresent(cb, ""+id);
        receiverSideSubsMap.put(sid,localSubs);

        if ( pred instanceof RLNoQueryPredicate ) {
            localSubs.getReceiver().receive(RLUtil.get().done());
            filterProcessor.startListening(localSubs);
        } else {
            FilterSpore spore = new FilterSpore(localSubs.getFilter()).modifiesResult(false);
            spore.onFinish( () -> localSubs.getReceiver().receive(RLUtil.get().done()) );
            spore.setForEach((r, e) -> {
                if (Actors.isResult(e)) {
                    localSubs.getReceiver().receive(new AddMessage(0,(Record) r));
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
    }

    public IPromise _setMapping(ClusterTableRecordMapping mapping) {
        this.mapping = mapping;
        return resolve(true);
    }

    @CallerSideMethod public ClusterTableRecordMapping getMapping() {
        return getActor().mapping;
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
        if ( s instanceof FilterSpore && ((FilterSpore) s).getFilter() instanceof RLHashIndexPredicate ) {
            processHashedFilter(s);
        } else if ( s instanceof FilterSpore &&
            ((FilterSpore) s).getFilter() instanceof QueryPredicate &&
            ((QueryPredicate<Record>) ((FilterSpore) s).getFilter()).getCompiled().getHashIndex() != null
        ) {
            FilterSpore fisp = (FilterSpore) s;
            QueryPredicate p = (QueryPredicate) fisp.getFilter();
            // check wether this is an axtual index
            String indexString = p.getCompiled().getHashIndex().getPath(0).getPathString();
            if ( indexedStorage.getHashIndex(indexString) != null ) {
                Log.Info(this,"detected index use in query "+p.getQuery());
                // reminder: check if sideeffects matter in case of weird behaviour
                fisp._setFilter(p.getCompiled().getHashIndex());
                processHashedFilter(fisp);
            } else {
                queuedSpores.add(new QueryQEntry(s, r));
                delayed(1, () -> _execQueriesOrDelay(queuedSpores.size(), taCount) );
            }
        } else {
            queuedSpores.add(new QueryQEntry(s, r));
            delayed(1, () -> _execQueriesOrDelay(queuedSpores.size(), taCount) );
        }
    }

    private void processHashedFilter(Spore s) {
        long tim = System.currentTimeMillis();
        RLHashIndexPredicate pathes = (RLHashIndexPredicate) ((FilterSpore) s).getFilter();
        Stream<String> keys;
        if ( pathes.getPath().size() == 1 ) {
            RLHashIndexPredicate.RLPath path = pathes.getPath(0);
            HashIndex idx = indexedStorage.getHashIndex( path.getPathString());
            if ( idx == null ) {
                s.complete(null,"hashIndex "+ path.getPathString()+" not found");
                return;
            }
            keys = idx.getKeys(path.getKey());
        } else {
            Set<String> res = null;
            for (int i = 0; i < pathes.getPath().size(); i++) {
                RLHashIndexPredicate.RLPath path = pathes.getPath(i);
                HashIndex idx = indexedStorage.getHashIndex(path.getPathString());
                if ( idx == null ) {
                    s.complete(null,"hashIndex "+path.getPathString()+" not found");
                    return;
                }
                Set<String> keySet = idx.getKeySet(path.getKey());
                if ( res == null )
                    res = new HashSet<>(Math.max(keySet.size(),800));
                if ( path instanceof RLHashIndexPredicate.JoinPath ) {
                    res.addAll(keySet);
                } else if ( path instanceof RLHashIndexPredicate.SubtractPath ) {
                    res.removeAll(keySet);
                } else if ( path instanceof RLHashIndexPredicate.IntersectionPath ) {
                    res.retainAll(keySet);
                }
            }
            keys = res.stream();
        }
        keys.forEach(key -> {
            Record record = storageDriver.getStore().get(key);
            if ( record != null ) {
                try {
                    s.remote(record);
                } catch (Exception e) {
                    s.complete(null,e);
                }
            } else {
                Log.Error(this,"corrupted index cannot find "+key);
            }
        });
        s.finish();
        if (DUMP_QUERY_TIME)
            Log.Info(this,"hashed query on "+description.getName()+"::"+pathes+" "+(System.currentTimeMillis()-tim));
    }

    public void _execQueriesOrDelay(int size, int taCount) {
        long tim = System.currentTimeMillis();
        Consumer<Record> recordConsumer = rec -> {
            for (int i = 0; i < queuedSpores.size(); i++) {
                QueryQEntry qqentry = queuedSpores.get(i);
                Spore spore = qqentry.spore;
                if (!spore.isFinished()) {
                    try {
                        spore.remote(rec);
                    } catch (Throwable ex) {
                        Log.Warn(this,ex,"exception in spore "+spore);
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
        if (DUMP_QUERY_TIME && queuedSpores.size() > 0)
            System.out.println("tim for "+queuedSpores.size()+" "+(System.currentTimeMillis()-tim)+" per q:"+(System.currentTimeMillis()-tim)/queuedSpores.size());
        queuedSpores.clear();
        return;
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

    @Override
    public void unsubscribeById(int subsId) {
        _unsubscribe( (r,e) -> {}, subsId );
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
    public IPromise atomic(int senderId, String key, RLFunction<Record, Object> action) {
        taCount++;
        return storageDriver.atomicQuery(senderId, key,action);
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
    public void put(int senderId, String key, Object... keyVals) {
        receive(RLUtil.get().put(senderId, key, keyVals));
    }

    @Override
    public void merge(int senderId, String key, Object... keyVals) {
        if ( ((Object)key) instanceof Record )
            throw new RuntimeException("probably accidental method resolution fail. Use merge instead");
        receive(RLUtil.get().addOrUpdate(senderId, key, keyVals));
    }

    @Override
    public IPromise<Boolean> add(int senderId, String key, Object... keyVals) {
        if ( storageDriver.getStore().get(key) != null )
            return resolve(false);
        receive(RLUtil.get().add(senderId, key, keyVals));
        return resolve(true);
    }

    @Override
    public IPromise<Boolean> addRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper)
            rec = ((RecordWrapper) rec).getRecord();
        Record existing = storageDriver.getStore().get(rec.getKey());
        if ( existing != null )
            return resolve(false);
        receive((ChangeMessage) new AddMessage(senderId,rec));
        return resolve(true);
    }

    @Override
    public void mergeRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receive(new AddMessage(senderId, true,rec));
    }

    @Override
    public void setRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receive(new PutMessage(senderId, rec));
    }

    @Override
    public void update(int senderId, String key, Object... keyVals) {
        receive(RLUtil.get().update(senderId, key, keyVals));
    }

    @Override
    public IPromise<Record> take(int senderId, String key) {
        Record record = storageDriver.getStore().get(key);
        receive(RLUtil.get().remove(senderId,key));
        return resolve(record);
    }

    @Override
    public void remove(int senderId, String key) {
        RemoveMessage remove = RLUtil.get().remove(senderId, key);
        receive(remove);
    }

    public void _removeSilent(String key) {
        storageDriver.getStore().remove(key);
    }

    public void _addSilent(Record rec) {
        storageDriver.getStore()._put(rec.getKey(),rec);
    }

    public IPromise<TableState> getTableState() {
        return resolve(
            new TableState(mapping,storageDriver.getStore().size(),description.getName())
        );
    }

}
