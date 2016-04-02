package org.nustaq.reallive.impl;

import org.nustaq.reallive.impl.actors.RealLiveTableActor;
import org.nustaq.reallive.interfaces.*;
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
public class FilterProcessor<K> implements ChangeReceiver<K> {

    List<Subscriber<K>> filterList = new ArrayList<>();
    RealLiveTableActor<K> table;

    public FilterProcessor(RealLiveTableActor<K> table) {
        this.table = table;
    }

    public void startListening(Subscriber<K> subs) {
        filterList.add(subs);
    }

    public void unsubscribe( Subscriber<K> subs ) {
        filterList.remove(subs);
    }

    public void receive(ChangeMessage<K> change) {
        switch (change.getType()) {
            case ChangeMessage.QUERYDONE:
                break;
            case ChangeMessage.PUT:
                processPut((PutMessage<K>) change);
                break;
            case ChangeMessage.ADD:
                processAdd((AddMessage<K>) change);
                break;
            case ChangeMessage.UPDATE:
                processUpdate((UpdateMessage<K>) change);
                break;
            case ChangeMessage.REMOVE:
                processRemove((RemoveMessage) change);
                break;
        }
    }

    protected void processPut(PutMessage<K> change) {
        Record<K> record = change.getRecord();
        for ( Subscriber<K> subscriber : filterList ) {
            if ( subscriber.getFilter().test(record) ) {
                subscriber.getReceiver().receive(change);
            }
        }
    }

    protected void processUpdate(UpdateMessage<K> change) {
        Record<K> newRecord = change.getNewRecord();
        String[] changedFields = change.getDiff().getChangedFields();
        Object[] oldValues = change.getDiff().getOldValues();
        Record oldRec = new RecordWrapper(newRecord) {
            @Override
            public Object get(String field) {
                int index = ChangeUtils.indexOf(field, changedFields);
                if ( index >= 0 ) {
                    return oldValues[index];
                }
                return super.get(field);
            }
        };
        for ( Subscriber<K> subscriber : filterList ) {
            boolean matchesOld = subscriber.getPrePatchFilter().test((Record<K>) oldRec);
            boolean matchesNew = subscriber.getPrePatchFilter().test(newRecord);

            if ( matchesNew ) {
                final PatchingRecord patchingRecord = FilterSpore.rec.get();
                patchingRecord.reset(newRecord);
                matchesNew = subscriber.getFilter().test(newRecord);
                newRecord = patchingRecord.unwrapOrCopy();
            }

            if ( matchesOld ) {
                final PatchingRecord patchingRecord = FilterSpore.rec.get();
                patchingRecord.reset(oldRec);
                matchesOld = subscriber.getFilter().test(oldRec);
                oldRec = patchingRecord.unwrapOrCopy();
            }

            // commented conditions are redundant
            if ( matchesOld && matchesNew) {
                subscriber.getReceiver().receive(change);
            } else if ( matchesOld /*&& ! matchesNew*/ ) {
                subscriber.getReceiver().receive(new RemoveMessage<>((Record)newRecord));
            } else if ( /*! matchesOld &&*/ matchesNew ) {
                subscriber.getReceiver().receive(new AddMessage<>(newRecord));
            }
        }
    }

    protected void processAdd(AddMessage<K> add) {
        Record<K> record = add.getRecord();
        for ( Subscriber<K> subscriber : filterList ) {
            if ( subscriber.getPrePatchFilter().test(record) ) {
                final PatchingRecord patchingRecord = FilterSpore.rec.get();
                patchingRecord.reset(record);
                if ( subscriber.getFilter().test(patchingRecord))
                    subscriber.getReceiver().receive(new AddMessage<K>(add.isUpdateIfExisting(),patchingRecord.unwrapOrCopy()));
            }
        }
    }

    protected void processRemove(RemoveMessage remove) {
        Record record = remove.getRecord();
        for ( Subscriber<K> subscriber : filterList ) {
            if ( subscriber.getPrePatchFilter().test(record) ) {
                final PatchingRecord patchingRecord = FilterSpore.rec.get();
                patchingRecord.reset(record);
                if ( subscriber.getFilter().test(patchingRecord))
                    subscriber.getReceiver().receive((ChangeMessage<K>)remove);
            }
        }
    }

}
