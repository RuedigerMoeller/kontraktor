package org.nustaq.reallive.client;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.server.FilterProcessor;
import org.nustaq.reallive.server.FilterSpore;
import org.nustaq.reallive.server.dynamic.DynClusterDistribution;
import org.nustaq.reallive.server.storage.StorageStats;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.server.RLUtil;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.QueryDoneMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.nustaq.reallive.api.Record;

/**
 * Created by moelrue on 06.08.2015.
 * Provides a single view on to a sharded table client side
 */
public class ShardedTable implements RealLiveTable {
    public static boolean DUMP_IN_PROC_CHANGES = false;

    final ConcurrentHashMap<Subscriber,List<Subscriber>> subsMap = new ConcurrentHashMap(); // real subscriptions only

    protected TableDescription description;
    protected HashMap<Integer,RealLiveTable> tableShardMap = new HashMap();
    protected Set<RealLiveTable> shards = new HashSet<>();
    protected FilterProcessor proc = new FilterProcessor(this);
    protected AtomicBoolean globalListenReady = new AtomicBoolean(false);

    public ShardedTable(RealLiveTable[] shards, TableDescription desc) {
        this.description = desc;
        for (int i = 0; i < shards.length; i++) {
            addNode( shards[i] );
        }
        long now = System.currentTimeMillis() - 8;
        realSubs( rec -> rec.getLastModified() > now, change -> globalListen(change) );
    }

    // actually subscribes at datanodes
    private Subscriber realSubs(RLPredicate<Record> filter, ChangeReceiver receiver) {
        Subscriber subs = new Subscriber(filter,receiver);
        this.realSubscribe(subs);
        return subs;
    }

    // actually subscribes at datanodes
    private void realSubscribe(Subscriber subs) {
        AtomicInteger doneCount = new AtomicInteger(shards.size());
        ChangeReceiver receiver = subs.getReceiver();
        ArrayList<Subscriber> subsList = new ArrayList();
        shards.forEach( shard -> {
            Subscriber shardSubs = new Subscriber(subs.getFilter(), change -> {
                if (change.getType() == ChangeMessage.QUERYDONE) {
                    int count = doneCount.decrementAndGet();
                    if (count == 0) {
                        receiver.receive(change);
                    }
                } else {
                    receiver.receive(change);
                }
            });
            subsList.add(shardSubs);
            shard.subscribe(shardSubs);
        });
        subsMap.put(subs,subsList);
    }

    private void globalListen(ChangeMessage change) {
        boolean fin = globalListenReady.get();
        if ( !fin ) {
            if (change.isDoneMsg()) {
                if (DUMP_IN_PROC_CHANGES) {
                    Log.Info(this, "Global Listen Ready");
                }
                globalListenReady.set(true);
            }
        }
        else if (fin) {
            if ( DUMP_IN_PROC_CHANGES ) {
                Log.Info(this, "Listen Receive:"+change);
            }
            proc.receive(change);
        } else {
            Log.Error(this, "Unexpected change routing:"+change);
            int shouldnotHppen = 1;
        }
    }

    public void addNode(RealLiveTable shard) {
        shards.add(shard);
    }

    public void removeTableShard(RealLiveTable shard2Remove) {
        for (Iterator<Map.Entry<Integer, RealLiveTable>> iterator = tableShardMap.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<Integer, RealLiveTable> next = iterator.next();
            if ( next.getValue() == shard2Remove ) {
                tableShardMap.remove(next.getKey());
                shards.remove(shard2Remove);
                return;
            }
        }
    }

    protected RealLiveTable getTableForKey(String key) {
        int h = Math.abs(key.hashCode())% tableShardMap.size();
        RealLiveTable table = tableShardMap.get(h);
        if ( table == null ) {
            Log.Warn(this, "cannot map keyHash " + h);
            return null; // FIXME: needs to be handled in methods below
        }
        return table;
    }

    @Override
    public void receive(ChangeMessage change) {
        if ( change.getType() != ChangeMessage.QUERYDONE )
            getTableForKey(change.getKey()).receive(change);
    }

    public IPromise resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        List<IPromise<Object>> futs = new ArrayList();
        shards.forEach( shard -> futs.add(shard.resizeIfLoadFactorLarger(loadFactor,maxGrowBytes)) );
        return Actors.all(futs);
    }

    @Override
    public void subscribe(Subscriber subs) {
        if ( subs.getFilter() instanceof RLNoQueryPredicate ) {
            subs.getReceiver().receive(new QueryDoneMessage());
            proc.startListening(subs);
        } else {
            proc.startListening(subs);
            forEach(subs.getFilter(), (change, err) -> {
                if (Actors.isResult(err)) {
                    subs.getReceiver().receive(new AddMessage(0,change));
                } else if (Actors.isComplete(err)) {
                    subs.getReceiver().receive(new QueryDoneMessage());
                }
            });
        }
    }

    protected void adjustLimitFilter(RLPredicate filter) {
        if ( filter instanceof RLLimitedPredicate) {
            ((RLLimitedPredicate)filter)._setLimit(Math.max(1,((RLLimitedPredicate) filter).getRecordLimit()/shards.size()));
        }
    }

    @Override
    public void unsubscribe(Subscriber subs) {
        proc.unsubscribe(subs);
    }

    @Override
    public void unsubscribeById(int subsId) {
        proc.unsubscribeById(subsId);
    }

    public void realUnsubscribe(Subscriber subs) {
        if ( subs == null ) {
            Log.Warn(this,"unsubscribed is null");
            return;
        }
        List<Subscriber> subscribers = subsMap.get(subs);
        if ( subscribers == null ) {
            Log.Warn(this,"unknown subscriber to unsubscribe "+subs);
            return;
        }
        for (int i = 0; i < subscribers.size(); i++) {
            Subscriber subscriber = subscribers.get(i);
            shards.forEach(shard->shard.unsubscribe(subscriber));
        }
        subsMap.remove(subs);
    }

    @Override
    public IPromise atomic(int senderId, String key, RLFunction<Record, Object> action) {
        return getTableForKey(key).atomic(senderId, key, action);
    }

    @Override
    public void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        shards.forEach( shard -> shard.atomicUpdate(filter,action) );
    }

    public void put(int senderId, String key, Object... keyVals) {
        getTableForKey(key).receive(new PutMessage(senderId,RLUtil.get().record(key,keyVals)));
    }

    public void merge(int senderId, String key, Object... keyVals) {
        getTableForKey(key).receive(RLUtil.get().addOrUpdate(senderId, key, keyVals));
    }

    public IPromise<Boolean> add(int senderId, String key, Object... keyVals) {
        return getTableForKey(key).add(senderId, key, keyVals);
    }

    public IPromise<Boolean> addRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        return getTableForKey(rec.getKey()).addRecord(senderId, rec);
    }

    public void mergeRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        getTableForKey(rec.getKey()).receive(new AddMessage(senderId, true,rec));
    }

    public void setRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        getTableForKey(rec.getKey()).receive(new PutMessage(senderId, rec));
    }

    public void update(int senderId, String key, Object... keyVals) {
        getTableForKey(key).receive(RLUtil.get().update(senderId, key, keyVals));
    }


    @Override
    public IPromise<Record> take(int senderId, String key) {
        return getTableForKey(key).take(senderId,key);
    }

    public void remove(int senderId, String key) {
        RemoveMessage remove = RLUtil.get().remove(senderId, key);
        getTableForKey(key).receive(remove);
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        if ( spore instanceof FilterSpore)
            adjustLimitFilter(((FilterSpore) spore).getFilter());
        spore.setExpectedFinishCount(shards.size());
        shards.forEach( shard -> shard.forEachWithSpore(spore) );
    }

    @Override
    public IPromise ping() {
        List<IPromise<Object>> futs = new ArrayList();
        return Actors.all( shards.stream().map( shard -> shard.ping() ).collect(Collectors.toList()));
    }

    @Override
    public IPromise<TableDescription> getDescription() {
        return new Promise(description);
    }

    public void stop() {
        shards.forEach( shard -> shard.stop() );
    }

    @Override
    public IPromise<StorageStats> getStats() {
        Promise res = new Promise();
        try {
            Actors.all(shards.stream().map( shard -> shard.getStats() ).collect(Collectors.toList()))
                    .then( (shardStats,err) -> {
                        if (shardStats!=null) {
                            StorageStats stats = new StorageStats();
                            shardStats.stream().map( fut -> fut.get() ).forEach( nodeStats -> nodeStats.addTo(stats));
                            res.resolve(stats);
                        } else {
                    res.reject(err);
                }
            });
        } catch (Exception e) {
            Log.Warn(this,e);
            res.reject(e);
        }
        return res;
    }

    @Override
    public IPromise<Record> get(String key) {
        if ( key == null )
            return null;
        return getTableForKey(key).get(key);
    }

    @Override
    public IPromise<Long> size() {
        Promise result = new Promise();
        Actors.all(shards.stream().map( shard -> shard.size() ).collect(Collectors.toList()))
                .then( longPromisList -> {
                    long sum = longPromisList.stream().mapToLong(prom -> prom.get()).sum();
                    result.resolve(sum);
                });
        return result;
    }

    public void removeNode(Actor actorRef) {
        shards.stream().filter( tableShard -> {
            Actor removedNode = actorRef.getActorRef();
            Actor shardFacade = ((Actor) tableShard).__clientConnection.getFacadeProxy().getActorRef();
            return shardFacade == removedNode;
        }).collect(Collectors.toList()).forEach( tableShard ->  {
            removeTableShard(tableShard);
        });
    }
}
