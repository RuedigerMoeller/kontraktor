package org.nustaq.kontraktor.reactivestreams;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.InThread;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.ActorPublisher;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serializable;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 28/06/15.
 */
public class PublisherActor<IN, OUT> extends Actor<PublisherActor<IN, OUT>> implements Processor<IN, OUT> {

    Map<Integer, SubscriberEntry> subscribers;
    int subsIdCount = 1;

    Function<IN,OUT> processor;

    public void setProcessor( Function<IN,OUT> processor ) {
        this.processor = processor;
    }

    @Override
    public void subscribe(@InThread Subscriber<? super OUT> subscriber) {
        if ( subscribers == null )
            subscribers = new HashMap<>();
        int id = subsIdCount++;
        KSubscription subs = new KSubscription(self(), id);
        subscribers.put( id, new SubscriberEntry(id, subs, subscriber) );
        subscriber.onSubscribe(subs);
    }

    public void _cancel(int id) {
        subscribers.remove(id);
    }

    public void _rq(long l, int id) {
        SubscriberEntry se = getSE(id);
        if ( se != null ) // ignore unknown subscribers
            se.addCredits(l);
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
        this.requestNextTrigger = batchSize/2;
    }

    @Override
    public void onSubscribe(Subscription subscription) {
        mySubs = subscription;
        emitRequestNext();
    }

    protected void emitRequestNext() {
        if ( openRequested < requestNextTrigger ) {
            mySubs.request(batchSize);
            openRequested += batchSize;
        }
    }

    @Override
    public void onNext(IN in) {
        openRequested--;
        try {
            OUT apply = processor.apply(in);
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
                entry.getSubscriber().onError(err);
            }
        });
    }

    protected void forwardMessage(Object msg) {
        if (subscribers == null) {
            return;
        }
        boolean blocked[] = {false};
        subscribers.forEach( (id,entry) -> {
            entry.sendPending();
            if ( entry.credits > 0 ) {
                entry.getSubscriber().onNext(msg);
                entry.decCredits();
            } else {
                entry.addPending(msg);
                blocked[0] = true;
            }
        });
        if ( ! blocked[0] ) {
            emitRequestNext();
        }
    }

    @Override
    public void onError(Throwable throwable) {
        forwardError(throwable);
    }

    @Override
    public void onComplete() {
        mySubs.cancel();
        subscribers.forEach( (id,entry) -> {
            if ( entry.credits > 0 ) {
                entry.getSubscriber().onComplete();
            }
        });
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
        protected Subscriber subscriber;
        protected LinkedList pending;

        public SubscriberEntry(int subsId, KSubscription subscription, Subscriber subscriber) {
            this.subsId = subsId;
            this.subscription = subscription;
            this.subscriber = subscriber;
            this.pending = new LinkedList<>();
        }

        public void addCredits(long l) {
            credits += l;
        }

        public void decCredits() {
            credits--;
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

        public Subscriber getSubscriber() {
            return subscriber;
        }

        public void sendPending() {
            int max = (int) Math.min(credits,pending.size());
            for (int i = 0; i < max; i++) {
                Object msg = pending.removeLast();
                subscriber.onNext(msg);
            }
        }

        public void addPending(Object msg) {
            pending.addFirst(msg);
        }
    }

}
