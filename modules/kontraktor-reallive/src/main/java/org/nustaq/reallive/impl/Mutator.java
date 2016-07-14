package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.AddMessage;
import org.nustaq.reallive.messages.PutMessage;
import org.nustaq.reallive.messages.RemoveMessage;
import org.nustaq.reallive.records.RecordWrapper;

/**
 * Created by ruedi on 08.08.2015.
 */
public class Mutator<K> implements Mutation<K> {
    ChangeReceiver<K> receiver;

    public Mutator(ChangeReceiver<K> receiver) {
        this.receiver = receiver;
    }

    @Override
    public IPromise<Boolean> putCAS(RLPredicate<Record<K>> casCondition, K key, Object... keyVals) {
        if ( receiver instanceof RealLiveTable) {
            return ((RealLiveTable) receiver).putCAS(casCondition,key,keyVals);
        }
        return new Promise<>(null, "unsupported operation");
    }

    @Override
    public void put(K key, Object ... keyVals) {
        receiver.receive(RLUtil.get().put(key, keyVals));
    }

    @Override
    public void atomic(K key, RLConsumer action) {
        if ( receiver instanceof RealLiveTable) {
            ((RealLiveTable) receiver).atomic(key, action);
            return;
        }
        throw new RuntimeException("unsupported operation");
    }

    @Override
    public IPromise atomicQuery(K key, RLFunction<Object, Record<K>> action) {
        if ( receiver instanceof RealLiveTable) {
            return ((RealLiveTable) receiver).atomicQuery(key, action);
        }
        return new Promise<>(null, "unsupported operation");
    }

    @Override
    public void addOrUpdate(K key, Object... keyVals) {
        if ( key instanceof Record )
            throw new RuntimeException("probably accidental method resolution fail. Use addOrUpdateRec instead");
        receiver.receive(RLUtil.get().addOrUpdate(key, keyVals));
    }

    @Override
    public void add(K key, Object... keyVals) {
        receiver.receive(RLUtil.get().add(key, keyVals));
    }

    @Override
    public void add(Record<K> rec) {
        if ( rec instanceof RecordWrapper)
            rec = ((RecordWrapper) rec).getRecord();
        receiver.receive((ChangeMessage<K>) new AddMessage<>(rec));
    }

    @Override
    public void addOrUpdateRec(Record<K> rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receiver.receive(new AddMessage<K>(true,rec));
    }

    @Override
    public void put(Record<K> rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receiver.receive(new PutMessage<K>(rec));
    }

    @Override
    public void update(K key, Object... keyVals) {
        receiver.receive(RLUtil.get().update(key, keyVals));
    }

    @Override
    public void remove(K key) {
        RemoveMessage remove = RLUtil.get().remove(key);
        receiver.receive(remove);
    }
}
