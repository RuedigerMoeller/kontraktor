package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.*;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.util.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serializable;
import java.util.*;
import java.util.Map.*;
import java.util.function.Function;

/**
 * Created by ruedi on 28/06/15.
 *
 * Inplements reactive streams async (queued) publisher. Don't use this class directly,
 * use the EventSink/ReaktiveStreams instead.
 *
 */
public class PublisherActor<IN, OUT> extends Actor<PublisherActor<IN, OUT>> implements Processor<IN, OUT>, KPublisher<OUT>, RemotedActor {

    public static int MAX_PENDING_MSG_BUFFERED = 2_000_000;

    public static final boolean CRED_DEBUG = false;

    protected Map<Integer, SubscriberEntry> subscribers;
    protected int subsIdCount = 1;

    protected Function<IN,OUT> processor;
    protected ArrayList<Runnable> doOnSubscribe = new ArrayList<>();
    protected ArrayDeque pending;

    public void init(Function<IN, OUT> processor) {
        this.pending = new ArrayDeque<>();
        this.processor = processor;
    }

    // with remoting a copy of subscriber is sent. calls "onSubscribe" on that
    // will do nothing ;)
    // transforming it to callerside will execute this at client side,
    // and do all remote communication via standard elements like promise and callback
    @Override @CallerSideMethod
    public void subscribe(Subscriber<? super OUT> subscriber) {
        _subscribe( (res,err) -> {
            if ( isError(err) ) {
                subscriber.onError((Throwable) err);
            } else if ( isErrorOrComplete(err) ) {
                subscriber.onComplete();
            } else {
                subscriber.onNext((OUT)res);
            }
        }).then(subs -> {
            subscriber.onSubscribe(subs);
        });
    }

    // private.
    public IPromise<KSubscription> _subscribe( Callback subscriber ) {
        if ( subscribers == null )
            subscribers = new HashMap<>();
        int id = subsIdCount++;
        KSubscription subs = new KSubscription(self(), id);
        subscribers.put( id, new SubscriberEntry(id, subs, subscriber) );
        return new Promise<>(subs);
    }

    // private. remoted cancel
    public void _cancel(int id) {
        if ( doOnSubscribe != null ) {
            doOnSubscribe.add(() -> _cancel(id));
        } else
            subscribers.remove(id);
    }

    // private. remoted request next
    @AsCallback public void _rq(long l, int id) {
        if ( doOnSubscribe != null ) {
            doOnSubscribe.add( () -> _rq(l, id) );
        } else {
            SubscriberEntry se = getSE(id);
            if ( se != null ) {// ignore unknown subscribers
                se.addCredits(l);
            } else {
                Log.Warn(this, "ignored credits " + l + " on id " + id);
            }
            emitRequestNext();
        }
    }

    protected SubscriberEntry getSE(Integer i) {
        return subscribers.get(i);
    }

    ///////////////////// subscriber interface ////////////////////

    protected Subscription mySubs;
    protected long batchSize;
    protected long requestNextTrigger;
    protected long openRequested;

    public void setBatchSize(int batchSize ) {
        this.batchSize = batchSize;
        this.requestNextTrigger = batchSize/ReaktiveStreams.REQU_NEXT_DIVISOR;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        mySubs = subscription;
        ArrayList<Runnable> tmp = this.doOnSubscribe;
        this.doOnSubscribe = null;
        tmp.forEach(runnable -> runnable.run());
    }

    protected void emitRequestNext() {
        if ( openRequested < requestNextTrigger ) {
            for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
                Entry<Integer, SubscriberEntry> next = iterator.next();
                if ( next.getValue().credits < openRequested ) {
                    return;
                }
            }
            mySubs.request(batchSize);
            openRequested += batchSize;
        }
    }

    @Override
    public void onNext(IN in) {
        if ( subscribers == null )
            return;
        openRequested--;
        try {
            OUT apply = processor.apply(in);
            if ( apply != null )
                forwardMessage(apply);
        } catch (Throwable err) {
            err.printStackTrace();
            forwardError(err);
        }
    }

    protected void forwardError(Throwable err) {
        if (subscribers == null) {
            err.printStackTrace();
            return;
        }
        subscribers.forEach( (id,entry) -> {
            if ( entry.credits > 0 ) {
                entry.onError(err);
            }
        });
        // fixme: cancel
    }

    protected void forwardMessage(Object msg) {
        if (subscribers == null) {
            return;
        }

        long minCredits = Long.MAX_VALUE;
        SubscriberEntry minEntry = null;
        for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<Integer, SubscriberEntry> next = iterator.next();
            SubscriberEntry entry = next.getValue();
            if ( minEntry == null )
                minEntry = entry;
            if ( minCredits > entry.getCredits() ) {
                minCredits = entry.getCredits();
                minEntry = entry;
            }
        }

        if ( minCredits <= 0 ) {
            pending.addFirst(msg);
            return;
        }

        List toRemove = null;
        if ( pending.size() > 0 ) {
            pending.addFirst(msg);
            while ( pending.size() > 0 && minCredits > 0 ) {
                Object toSend = pending.removeLast();
                for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
                    Entry<Integer, SubscriberEntry> next = iterator.next();
                    SubscriberEntry entry = next.getValue();
                    try {
                        entry.onNext(toSend);
                    } catch (Throwable th) {
                        if ( toRemove == null ) {
                            toRemove = new ArrayList();
                        }
                        toRemove.add(entry);
                    }
                }
                minCredits--;
            }
        } else {
            for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
                Entry<Integer, SubscriberEntry> next = iterator.next();
                SubscriberEntry entry = next.getValue();
                try {
                    entry.onNext(msg);
                } catch (Throwable th) {
                    if ( toRemove == null ) {
                        toRemove = new ArrayList();
                    }
                    toRemove.add(entry);
                }
            }
            minCredits--;
        }
        if ( toRemove != null ) {
            toRemove.forEach( e -> {
                int subsId = ((SubscriberEntry) e).getSubsId();
                try {
                    _cancel(subsId);
                } catch (Throwable th) {
                    // ignore
                }
                subscribers.remove(subsId);
                subscriberDisconnected(subsId);
            } );
        }
        if ( subscribers.size() > 0 ) {
            emitRequestNext();
        } else {
            if ( openRequested > 0 || pending.size() > 0 ) {
                Log.Info(this, "no subscribers, deleting "+pending.size()+" messages");
                pending.clear();
            }
            openRequested = 0;
        }
    }

    @Override
    public void onError(Throwable throwable) {
        forwardError(throwable);
    }

    @Override
    public void onComplete() {
        mySubs.cancel();
        subscribers.forEach((id, entry) -> {
            if (entry.credits > 0) {
                entry.onComplete();
            }
        });
    }

    @Override // some client disconnected
    public void hasBeenUnpublished() {
        for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<Integer, SubscriberEntry> entry = iterator.next();
            Callback subscriber = entry.getValue().subscriber;
            if ( subscriber instanceof CallbackWrapper &&
                 ((CallbackWrapper) subscriber).isTerminated() ) {
                iterator.remove();
                subscriberDisconnected(entry.getKey());
            }
        }
    }

    public void subscriberDisconnected(int id) {
        Log.Info(this, "a stream client disconnected id:" + id + " remaining:" + subscribers.size());
        emitRequestNext();
    }

    protected static class KSubscription implements Subscription, Serializable {

        protected PublisherActor publisher; // actorref
        protected int id;

        public KSubscription(PublisherActor publisher, int id) {
            this.publisher = publisher;
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public void request(long l) {
            publisher._rq(l, id);
        }

        @Override
        public void cancel() {
            publisher._cancel(id);
        }

    }

    protected static class SubscriberEntry {
        protected int subsId;
        protected long credits;
        protected KSubscription subscription;
        protected Callback subscriber;

        public SubscriberEntry(int subsId, KSubscription subscription, Callback subscriber) {
            this.subsId = subsId;
            this.subscription = subscription;
            this.subscriber = subscriber;
        }

        public void addCredits(long l) {
            credits += l;
            if ( CRED_DEBUG )
                System.out.println("id "+subsId+" got credits, has:"+credits);
        }

        public int getSubsId() {
            return subsId;
        }

        public long getCredits() {
            return credits;
        }

        public KSubscription getSubscription() {
            return subscription;
        }

        public void onError(Throwable err) {
            subscriber.reject(err);
        }

        public void onNext(Object msg) {
            subscriber.stream(msg);
            credits--;
        }

        public void onComplete() {
            subscriber.finish();
        }
    }

}
