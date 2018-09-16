package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.FilterProcessor;
import org.nustaq.reallive.impl.storage.StorageStats;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.QueryDoneMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.stream.Collectors;

/**
 * Created by moelrue on 06.08.2015.
 * Provides a single view on to a sharded table
 */
public class ShardedTable implements RealLiveTable {
    final static int NUM_SLOTS = 10_000;

    final ConcurrentHashMap<Subscriber,List<Subscriber>> subsMap = new ConcurrentHashMap(); // real subscriptions only
    private TableDescription description;
    private HashMap<Integer,RealLiveTable[]> shardMap = new HashMap();
    private Set<RealLiveTable> shards = new HashSet<>();
    private FilterProcessor proc = new FilterProcessor(this);
    AtomicBoolean globalListenReady = new AtomicBoolean(false);

    public ShardedTable(RealLiveTable[] shards, TableDescription desc) {
        this.description = desc;
        for (int i = 0; i < shards.length; i++) {
            addNode( createSlots(shards.length,i), shards[i] );
        }
        if ( ! isComplete() ) {
            Log.Error(this,"incomplete key coverage");
        }

        long now = System.currentTimeMillis();
        realSubs( (RLNoQueryPredicate)rec -> true, change -> globalListen(change) );
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
        if ( !fin && change.isDoneMsg() ) {
            globalListenReady.set(true);
        }
        else if (fin) {
            proc.receive(change);
        } else {
            int shouldnotHppen = 1;
        }
    }

    private void dumpMisses() {
        for (int i=0; i < NUM_SLOTS; i++ ) {
            if ( ! shardMap.containsKey(i) )
                Log.Error(this,"   missing bucket "+i);
        }
    }

    private boolean isComplete() {
        for (int i=0; i < NUM_SLOTS; i++ ) {
            if ( ! shardMap.containsKey(i) )
                return false;
        }
        return true;
    }

    private int[] createSlots(int length, int i) {
        int start = (NUM_SLOTS/length) * i;
        int end = (NUM_SLOTS/length) * (i+1);
        if ( i == length - 1 ) // extends last node to remaining
            end = NUM_SLOTS;
        int arr[] = new int[end-start];
        for ( int ii = start; ii < end; ii ++ ) {
            arr[ii-start] = ii;
        }
        return arr;
    }

    public void addNode(int slots[], RealLiveTable shard) {
        shards.add(shard);
        for (int i = 0; i < slots.length; i++) {
            int slot = slots[i];
            addSlot(shard, slot);
        }
    }

    public void removeTableShard(RealLiveTable shard2Remove) {
        shardMap.forEach( (k,v) -> {
            for (int i = 0; i < v.length; i++) {
                RealLiveTable realLiveTable = v[i];
                if ( realLiveTable == shard2Remove ) {
                    RealLiveTable newArr[] = new RealLiveTable[v.length-1];
                    int oldIdx = 0; int newIdx = 0;
                    while( newIdx < newArr.length ) {
                        if ( v[oldIdx] != shard2Remove ) {
                            newArr[newIdx] = v[oldIdx];
                            newIdx++; oldIdx++;
                        } else {
                            oldIdx++;
                        }
                    }
                    break;
                }
            }
        });
        shards.remove(shard2Remove);
    }

    private void addSlot(RealLiveTable shard, int slot) {
        RealLiveTable[] mappedShards = shardMap.get(slot);
        if ( mappedShards == null ) {
            mappedShards = new RealLiveTable[] { shard };
            shardMap.put(slot, mappedShards);
        } else {
            RealLiveTable newArr[] = new RealLiveTable[mappedShards.length+1];
            System.arraycopy(mappedShards,0,newArr,0,mappedShards.length+1);
            newArr[newArr.length-1] = shard;
            shardMap.put(slot, newArr);
        }
    }

    protected RealLiveTable[] hashAll(String key) {
        int h = Math.abs(key.hashCode())%NUM_SLOTS;
        RealLiveTable[] tables = shardMap.get(h);
        if ( tables == null || tables.length == 0 ) {
            Log.Warn(this, "cannot map keyHash " + h);
            return null; // FIXME: needs to be handled in methods below
        }
        return tables;
    }

    protected RealLiveTable hashAny(String key) {
        int h = Math.abs(key.hashCode())%NUM_SLOTS;
        RealLiveTable[] tables = shardMap.get(h);
        if ( tables == null || tables.length == 0 ) {
            Log.Warn(this, "cannot map keyHash " + h);
            return null; // FIXME: needs to be handled in methods below
        }
        return tables[0];
    }

    @Override
    public void receive(ChangeMessage change) {
        if ( change.getType() != ChangeMessage.QUERYDONE )
            hashAny(change.getKey()).receive(change);
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

    @Override
    public void unsubscribe(Subscriber subs) {
        proc.unsubscribe(subs);
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
    public IPromise atomic(String key, RLFunction<Record, Object> action) {
        return hashAny(key).atomic(key, action);
    }

    @Override
    public void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        shards.forEach( shard -> shard.atomicUpdate(filter,action) );
    }

    public void put(int senderId, String key, Object... keyVals) {
        hashAny(key).receive(new PutMessage(senderId,RLUtil.get().record(key,keyVals)));
    }

    public void merge(int senderId, String key, Object... keyVals) {
        hashAny(key).receive(RLUtil.get().addOrUpdate(senderId, key, keyVals));
    }

    public IPromise<Boolean> add(int senderId, String key, Object... keyVals) {
        return hashAny(key).add(senderId, key, keyVals);
    }

    public IPromise<Boolean> addRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        return hashAny(rec.getKey()).addRecord(senderId, rec);
    }

    public void mergeRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        hashAny(rec.getKey()).receive(new AddMessage(senderId, true,rec));
    }

    public void setRecord(int senderId, Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        hashAny(rec.getKey()).receive(new PutMessage(senderId, rec));
    }

    public void update(int senderId, String key, Object... keyVals) {
        hashAny(key).receive(RLUtil.get().update(senderId, key, keyVals));
    }


    @Override
    public IPromise<Record> take(int senderId, String key) {
        return hashAny(key).take(senderId,key);
    }

    public void remove(int senderId, String key) {
        RemoveMessage remove = RLUtil.get().remove(senderId, key);
        hashAny(key).receive(remove);
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
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
        return hashAny(key).get(key);
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
