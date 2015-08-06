package org.nustaq.reallive.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.reallive.api.*;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class Sharding<K,V extends Record<K>> implements ChangeReceiver<K,V>, Mutation<K,V>, RecordIterable<K,V>, ChangeStream<K,V> {

    ShardFunc<K> func;
    RealLiveStreamActor<K,V> shards[];
    ConcurrentHashMap<Subscriber,List<Subscriber>> subsMap = new ConcurrentHashMap<>();

    @Override
    public void receive(ChangeMessage<K, V> change) {
        if ( change.getType() != ChangeMessage.QUERYDONE )
            shards[func.apply(change.getKey())].receive(change);
    }

    @Override
    public void subscribe(Subscriber<K, V> subs) {
        AtomicInteger doneCount = new AtomicInteger(shards.length);
        ChangeReceiver<K, V> receiver = subs.getReceiver();
        ArrayList<Subscriber> subsList = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            RealLiveStreamActor<K, V> shard = shards[i];
            Subscriber<K, V> shardSubs = new Subscriber<>(subs.getFilter(), change -> {
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
    public void unsubscribe(Subscriber<K, V> subs) {
        List<Subscriber> subscribers = subsMap.get(subs);
        for (int i = 0; i < subscribers.size(); i++) {
            Subscriber subscriber = subscribers.get(i);
            shards[i].unsubscribe(subscriber);
        }
        subsMap.remove(subs);
    }

    @Override
    public void add(K key, Object... keyVals) {
        shards[func.apply(key)].add(key,keyVals);
    }

    @Override
    public void update(K key, Object... keyVals) {
        shards[func.apply(key)].update(key,keyVals);
    }

    @Override
    public void remove(K key) {
        shards[func.apply(key)].remove(key);
    }

    @Override
    public void forEach(Predicate<V> filter, Consumer<V> action) {
        for (int i = 0; i < shards.length; i++) {
            RealLiveStreamActor<K, V> shard = shards[i];
            shard.forEach(filter, action );
        }
    }
}
