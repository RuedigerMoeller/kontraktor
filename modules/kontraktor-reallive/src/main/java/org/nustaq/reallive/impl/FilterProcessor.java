package org.nustaq.reallive.impl;

import org.nustaq.reallive.api.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.*;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by moelrue on 04.08.2015.
 *
 * A filtering listener allows for registration of filtered subscribers and
 * processes + transforms incoming changes on a per subscriber base:
 *
 * in: old record, new record
 * if ( filter matches old && ! new ) => send Remove
 * if ( filter matches old && new ) => send Update
 * if ( filter ! matches old && new ) => send Add
 */
public class FilterProcessor<K,V extends Record<K>> implements ChangeReceiver<K,V>, ChangeStream<K,V> {

    List<Subscriber<K,V>> filterList = new ArrayList<>();
    RecordStreamProvider<K,V> provider;

    public FilterProcessor(RecordStreamProvider<K, V> provider) {
        this.provider = provider;
    }

    @Override
    public void subscribe(Subscriber<K,V> subs) {
        filterList.add(subs);
        provider.forEach( record -> {
            if ( subs.getFilter().test(record) ) {
                subs.getReceiver().receive(new AddMessage<>(record));
            }
        });
    }

    public void unsubscribe( Subscriber<K,V> subs ) {
        filterList.remove(subs);
    }

    @Override
    public void receive(ChangeMessage<K, V> change) {
        switch (change.getType()) {
            case ChangeMessage.ADD:
                processAdd((AddMessage<K, V>) change);
                break;
            case ChangeMessage.UPDATE:
                processUpdate((UpdateMessage<K, V>) change);
                break;
            case ChangeMessage.REMOVE:
                processRemove((RemoveMessage) change);
                break;
        }
    }

    protected void processUpdate(UpdateMessage<K, V> change) {
        V newRecord = change.getNewRecord();
        String[] changedFields = change.getDiff().getChangedFields();
        Object[] oldValues = change.getDiff().getOldValues();
        Record oldRec = new PatchedRecord(newRecord) {
            @Override
            public Object get(String field) {
                int index = ChangeUtils.indexOf(field, changedFields);
                if ( index >= 0 ) {
                    return oldValues[index];
                }
                return super.get(field);
            }
        };
        for ( Subscriber<K,V> subscriber : filterList ) {
            boolean matchesOld = subscriber.getFilter().test((V) oldRec);
            boolean matchesNew = subscriber.getFilter().test(newRecord);
            // commented conditions are redundant
            if ( matchesOld && matchesNew) {
                subscriber.getReceiver().receive(change);
            } else if ( matchesOld /*&& ! matchesNew*/ ) {
                subscriber.getReceiver().receive(new RemoveMessage<>((Record)change.getNewRecord()));
            } else if ( /*! matchesOld &&*/ matchesNew ) {
                subscriber.getReceiver().receive(new AddMessage<>(change.getNewRecord()));
            }
        }
    }

    protected void processAdd(AddMessage<K,V> add) {
        V record = add.getRecord();
        for ( Subscriber<K,V> subscriber : filterList ) {
            if ( subscriber.getFilter().test(record) ) {
                subscriber.getReceiver().receive(add);
            }
        }
    }

    protected void processRemove(RemoveMessage remove) {
        Record record = remove.getRecord();
        for ( Subscriber<K,V> subscriber : filterList ) {
            if ( subscriber.getFilter().test((V) record) ) { // if matched before, promote remove
                subscriber.getReceiver().receive((ChangeMessage<K, V>)remove);
            }
        }
    }

}
