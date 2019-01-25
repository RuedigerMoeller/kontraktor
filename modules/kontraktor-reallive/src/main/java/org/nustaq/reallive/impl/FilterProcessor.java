package org.nustaq.reallive.impl;

import org.nustaq.reallive.impl.actors.RealLiveTableActor;
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
public class FilterProcessor implements ChangeReceiver {

    List<Subscriber> filterList = new ArrayList();
    RealLiveTable table;

    public FilterProcessor(RealLiveTable table) {
        this.table = table;
    }

    //FIXME: benchmark if cloning is faster
    public synchronized void startListening( Subscriber subs ) {
        filterList.add(subs);
    }

    public synchronized void unsubscribe( Subscriber subs ) {
        filterList.remove(subs);
    }

    public synchronized void receive(ChangeMessage change) {
        switch (change.getType()) {
            case ChangeMessage.QUERYDONE:
                break;
            case ChangeMessage.PUT:
                processPut((PutMessage) change);
                break;
            case ChangeMessage.ADD:
                processAdd((AddMessage) change);
                break;
            case ChangeMessage.UPDATE:
                processUpdate((UpdateMessage) change);
                break;
            case ChangeMessage.REMOVE:
                processRemove((RemoveMessage) change);
                break;
        }
    }

    protected void processPut(PutMessage change) {
        Record record = change.getRecord();
        for ( Subscriber subscriber : filterList ) {
            if ( subscriber.getFilter().test(record) ) {
                subscriber.getReceiver().receive(change);
            }
        }
    }

    protected void processUpdate(UpdateMessage change) {
        Record newRecord = change.getNewRecord();
        String[] changedFields = change.getDiff().getChangedFields();
        Object[] oldValues = change.getDiff().getOldValues();
        Record oldRec = new PatchingRecord(newRecord);
        for (int i = 0; i < changedFields.length; i++) {
            String changedField = changedFields[i];
            oldRec.put(changedField, oldValues[i]);
        }
        for ( Subscriber subscriber : filterList ) {
            boolean matchesOld = subscriber.getFilter().test(oldRec);
            boolean matchesNew = subscriber.getFilter().test(newRecord);

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
                subscriber.getReceiver().receive(new RemoveMessage(change.getSenderId(),(Record)newRecord));
            } else if ( /*! matchesOld &&*/ matchesNew ) {
                subscriber.getReceiver().receive(new AddMessage(change.getSenderId(),newRecord));
            }
        }
    }

    protected void processAdd(AddMessage add) {
        Record record = add.getRecord();
        for ( Subscriber subscriber : filterList ) {
            if ( subscriber.getFilter().test(record) ) {
                final PatchingRecord patchingRecord = FilterSpore.rec.get();
                patchingRecord.reset(record);
                if ( subscriber.getFilter().test(patchingRecord))
                    subscriber.getReceiver().receive(new AddMessage(add.getSenderId(),add.isUpdateIfExisting(),patchingRecord.unwrapOrCopy()));
            }
        }
    }

    protected void processRemove(RemoveMessage remove) {
        Record record = remove.getRecord();
        for ( Subscriber subscriber : filterList ) {
            if ( subscriber.getFilter().test(record) ) {
                final PatchingRecord patchingRecord = FilterSpore.rec.get();
                patchingRecord.reset(record);
                if ( subscriber.getFilter().test(patchingRecord))
                    subscriber.getReceiver().receive((ChangeMessage)remove);
            }
        }
    }

    public void unsubscribeById(long subsId) {
        List<Subscriber> toRem = new ArrayList<>();
        for ( Subscriber subscriber : filterList ) {
            if ( subscriber.getId() == subsId ) {
                toRem.add(subscriber);
            }
        }
        toRem.forEach( s -> unsubscribe(s));
    }
}
