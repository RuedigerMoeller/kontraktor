package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.impl.storage.StorageStats;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class TableSharding implements RealLiveTable {

    RealLiveTable shards[];
    final ConcurrentHashMap<Subscriber,List<Subscriber>> subsMap = new ConcurrentHashMap();
    private TableDescription description;

    public TableSharding(RealLiveTable[] shards, TableDescription desc) {
        this.shards = shards;
        this.description = desc;
    }

    protected RealLiveTable hash(String key) {
        return shards[Math.abs(key.hashCode())%shards.length];
    }

    @Override
    public void receive(ChangeMessage change) {
        if ( change.getType() != ChangeMessage.QUERYDONE )
            hash(change.getKey()).receive(change);
    }

    public IPromise resizeIfLoadFactorLarger(double loadFactor, long maxGrowBytes) {
        List<IPromise<Object>> futs = new ArrayList();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable shard = shards[i];
            futs.add(shard.resizeIfLoadFactorLarger(loadFactor,maxGrowBytes));
        }
        return Actors.all(futs);
    }

    @Override
    public void subscribe(Subscriber subs) {
        AtomicInteger doneCount = new AtomicInteger(shards.length);
        ChangeReceiver receiver = subs.getReceiver();
        ArrayList<Subscriber> subsList = new ArrayList();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable shard = shards[i];
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
        }
        subsMap.put(subs,subsList);
    }

    @Override
    public void unsubscribe(Subscriber subs) {
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
            shards[i].unsubscribe(subscriber);
        }
        subsMap.remove(subs);
    }

    @Override
    public IPromise atomic(String key, RLFunction<Record, Object> action) {
        return hash(key).atomic(key, action);
    }

    @Override
    public void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        for (int i = 0; i < shards.length; i++) {
            shards[i].atomicUpdate(filter,action);
        }
    }

    public void put(String key, Object... keyVals) {
        hash(key).receive(new PutMessage(RLUtil.get().record(key,keyVals)));
    }

    public void merge(String key, Object... keyVals) {
        hash(key).receive(RLUtil.get().addOrUpdate(key, keyVals));
    }

    public IPromise<Boolean> add(String key, Object... keyVals) {
        return hash(key).add(key, keyVals);
    }

    public IPromise<Boolean> addRecord(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        return hash(rec.getKey()).addRecord(rec);
    }

    public void mergeRecord(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        hash(rec.getKey()).receive(new AddMessage(true,rec));
    }

    public void setRecord(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        hash(rec.getKey()).receive(new PutMessage(rec));
    }

    public void update(String key, Object... keyVals) {
        hash(key).receive(RLUtil.get().update(key, keyVals));
    }

    @Override
    public IPromise<Record> take(String key) {
        return hash(key).take(key);
    }

    public void remove(String key) {
        RemoveMessage remove = RLUtil.get().remove(key);
        hash(key).receive(remove);
    }

    @Override
    public <T> void forEachWithSpore(Spore<Record, T> spore) {
        spore.setExpectedFinishCount(shards.length);
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable shard = shards[i];
            shard.forEachWithSpore(spore);
        }
    }

    @Override
    public IPromise ping() {
        List<IPromise<Object>> futs = new ArrayList();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable shard = shards[i];
            futs.add(shard.ping());
        }
        return Actors.all(futs);
    }

    @Override
    public IPromise<TableDescription> getDescription() {
        return new Promise(description);
    }

    public void stop() {
        for (int i = 0; i < shards.length; i++) {
            shards[i].stop();
        }
    }

    @Override
    public IPromise<StorageStats> getStats() {
        Promise res = new Promise();
        try {
            Actors.all(shards.length, i -> shards[i].getStats()).then( (shardStats,err) -> {
                if (shardStats!=null) {
                    StorageStats stats = new StorageStats();
                    for (int i = 0; i < shardStats.length; i++) {
                        StorageStats storageStats = shardStats[i].get();
                        storageStats.addTo(stats);
                    }
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
        return hash(key).get(key);
    }

    @Override
    public IPromise<Long> size() {
        Promise result = new Promise();
        List<IPromise<Long>> futs = new ArrayList();
        for (int i = 0; i < shards.length; i++) {
            RealLiveTable shard = shards[i];
            futs.add(shard.size());
        }
        Actors.all(futs).then( longPromisList -> {
            long sum = longPromisList.stream().mapToLong(prom -> prom.get()).sum();
            result.resolve(sum);
        });
        return result;
    }
}
