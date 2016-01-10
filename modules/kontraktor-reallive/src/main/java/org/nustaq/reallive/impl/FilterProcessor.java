package org.nustaq.reallive.impl;

import org.nustaq.kontraktor.Actors;
import org.nustaq.reallive.interfaces.*;
import org.nustaq.reallive.messages.*;
import org.nustaq.reallive.records.*;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

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
public class FilterProcessor<K> implements ChangeReceiver<K>, ChangeStream<K> {

    List<Subscriber<K>> filterList = new ArrayList<>();
    RecordIterable<K> provider;

    public FilterProcessor(RecordIterable<K> provider) {
        this.provider = provider;
    }

    @Override
    public void subscribe(Subscriber<K> subs) {
        filterList.add(subs);
        FilterSpore spore = new FilterSpore(subs.getFilter());
        spore.onFinish( () -> subs.getReceiver().receive(RLUtil.get().done()) );
        provider.forEach(spore.forEach((r, e) -> {
            if (Actors.isResult(e)) {
                subs.getReceiver().receive(new AddMessage((Record) r));
            } else {
                // FIXME: pass errors also
                // FIXME: never called (see onFinish above)
                subs.getReceiver().receive(RLUtil.get().done());
            }
        }));
    }

    public void unsubscribe( Subscriber<K> subs ) {
        filterList.remove(subs);
    }

    @Override
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
            boolean matchesOld = subscriber.getFilter().test((Record<K>) oldRec);
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

    protected void processAdd(AddMessage<K> add) {
        Record<K> record = add.getRecord();
        for ( Subscriber<K> subscriber : filterList ) {
            if ( subscriber.getFilter().test(record) ) {
                subscriber.getReceiver().receive(add);
            }
        }
    }

    protected void processRemove(RemoveMessage remove) {
        Record record = remove.getRecord();
        for ( Subscriber<K> subscriber : filterList ) {
            if ( subscriber.getFilter().test((Record<K>) record) ) { // if matched before, promote remove
                subscriber.getReceiver().receive((ChangeMessage<K>)remove);
            }
        }
    }

}
