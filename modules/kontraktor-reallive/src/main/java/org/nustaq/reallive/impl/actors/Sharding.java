package org.nustaq.reallive.impl.actors;

import org.nustaq.kontraktor.*;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.impl.RLUtil;
import org.nustaq.reallive.messages.RemoveMessage;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import java.util.function.*;

/**
 * Created by moelrue on 06.08.2015.
 */
public class Sharding<K,V extends Record<K>> implements RealLiveTable<K,V> {

    ShardFunc<K> func;
    RealLiveStreamActor<K,V> shards[];
    final ConcurrentHashMap<Subscriber,List<Subscriber>> subsMap = new ConcurrentHashMap<>();

    public Sharding(ShardFunc<K> func, RealLiveStreamActor<K, V>[] shards) {
        this.func = func;
        this.shards = shards;
    }

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

    protected class ShardMutation implements Mutation<K,V> {
        @Override
        public void put(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().put(key, keyVals));
        }

        @Override
        public void addOrUpdate(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().addOrUpdate(key, keyVals));
        }

        @Override
        public void add(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().add(key, keyVals));
        }

        @Override
        public void update(K key, Object... keyVals) {
            shards[func.apply(key)].receive(RLUtil.get().update(key, keyVals));
        }

        @Override
        public void remove(K key) {
            RemoveMessage remove = RLUtil.get().remove(key);
            shards[func.apply(key)].receive(remove);
        }
    }

    public Mutation<K,V> getMutation() {
        return new ShardMutation();
    }

    @Override
    public void forEach(Predicate<V> filter, Consumer<V> action) {
        for (int i = 0; i < shards.length; i++) {
            RealLiveStreamActor<K, V> shard = shards[i];
            shard.forEach(filter, action );
        }
    }

    @Override
    public IPromise ping() {
        List<IPromise<Object>> futs = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            RealLiveStreamActor<K, V> shard = shards[i];
            futs.add(shard.ping());
        }
        return Actors.all(futs);
    }

    public void stop() {
        for (int i = 0; i < shards.length; i++) {
            shards[i].stop();
        }
    }

    @Override
    public IPromise<V> get(K key) {
        return shards[func.apply(key)].get(key);
    }

    @Override
    public IPromise<Long> size() {
        Promise result = new Promise();
        List<IPromise<Long>> futs = new ArrayList<>();
        for (int i = 0; i < shards.length; i++) {
            RealLiveStreamActor<K, V> shard = shards[i];
            futs.add(shard.size());
        }
        Actors.all(futs).then( longPromisList -> {
            long sum = longPromisList.stream().mapToLong(prom -> prom.get()).sum();
            result.resolve(sum);
        });
        return result;
    }
}
