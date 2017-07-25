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
public class Mutator implements Mutation {
    ChangeReceiver receiver;

    public Mutator(ChangeReceiver receiver) {
        this.receiver = receiver;
    }

    @Override
    public IPromise<Boolean> putCAS(RLPredicate<Record> casCondition, String key, Object... keyVals) {
        if ( receiver instanceof RealLiveTable) {
            return ((RealLiveTable) receiver).putCAS(casCondition,key,keyVals);
        }
        return new Promise(null, "unsupported operation");
    }

    @Override
    public void put(String key, Object ... keyVals) {
        receiver.receive(RLUtil.get().put(key, keyVals));
    }

    @Override
    public void atomic(String key, RLConsumer action) {
        if ( receiver instanceof RealLiveTable) {
            ((RealLiveTable) receiver).atomic(key, action);
            return;
        }
        throw new RuntimeException("unsupported operation");
    }

    @Override
    public IPromise atomicQuery(String key, RLFunction<Record,Object> action) {
        if ( receiver instanceof RealLiveTable) {
            return ((RealLiveTable) receiver).atomicQuery(key, action);
        }
        return new Promise(null, "unsupported operation");
    }

    @Override
    public void atomicUpdate(RLPredicate<Record> filter, RLFunction<Record, Boolean> action) {
        if ( receiver instanceof RealLiveTable) {
            ((RealLiveTable) receiver).atomicUpdate(filter, action);
        }
        throw new RuntimeException("unsupported operation");
    }

    @Override
    public void addOrUpdate(String key, Object... keyVals) {
        if ( ((Object)key) instanceof Record )
            throw new RuntimeException("probably accidental method resolution fail. Use addOrUpdateRec instead");
        receiver.receive(RLUtil.get().addOrUpdate(key, keyVals));
    }

    @Override
    public void add(String key, Object... keyVals) {
        receiver.receive(RLUtil.get().add(key, keyVals));
    }

    @Override
    public void add(Record rec) {
        if ( rec instanceof RecordWrapper)
            rec = ((RecordWrapper) rec).getRecord();
        receiver.receive((ChangeMessage) new AddMessage(rec));
    }

    @Override
    public void addOrUpdateRec(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receiver.receive(new AddMessage(true,rec));
    }

    @Override
    public void put(Record rec) {
        if ( rec instanceof RecordWrapper )
            rec = ((RecordWrapper) rec).getRecord();
        receiver.receive(new PutMessage(rec));
    }

    @Override
    public void update(String key, Object... keyVals) {
        receiver.receive(RLUtil.get().update(key, keyVals));
    }

    @Override
    public void remove(String key) {
        RemoveMessage remove = RLUtil.get().remove(key);
        receiver.receive(remove);
    }
}
