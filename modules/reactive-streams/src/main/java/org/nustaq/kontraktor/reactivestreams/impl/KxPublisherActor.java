/*
Kontraktor-reactivestreams Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/
package org.nustaq.kontraktor.reactivestreams.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.*;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import org.nustaq.kontraktor.util.Log;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

import java.io.Serializable;
import java.util.*;
import java.util.Map.*;
import java.util.concurrent.*;
import java.util.function.Function;

/**
 * Created by ruedi on 28/06/15.
 *
 * Implements reactive streams async (queued,multi subscriber) publisher. Don't use this class directly,
 * use the EventSink/ReaktiveStreams.get() instead.
 *
 */
public class KxPublisherActor<IN, OUT> extends Actor<KxPublisherActor<IN, OUT>> implements Processor<IN, OUT>, KxPublisher<OUT>, RemotedActor {

    public static final boolean CRED_DEBUG = false;

    protected Map<Integer, SubscriberEntry> subscribers;
    protected int subsIdCount = 1;

    protected Function<IN,OUT> processor;
    protected ArrayList<Runnable> doOnSubscribe = new ArrayList<>();
    protected ArrayDeque pending;
    protected boolean isIteratorBased = false;
    protected boolean closeOnComplete = false;
    protected Object actorServer;
    protected boolean lossy = false;
    public KxReactiveStreams _streams; // required public for remoting

    public void init(Function<IN, OUT> processor) {
//        this._streams = streams; MUST be set sync !!
        this.pending = new ArrayDeque<>();
        this.processor = processor;
    }

    /**
     * acts as an pull based event producer then
     * @param iterator
     */
    public void initFromIterator(Iterator<IN> iterator) {
        this.pending = new ArrayDeque<>();
        isIteratorBased = true;
        //this._streams = kxReactiveStreams; needs sync init !!
        // in case iterator is blocking, need a dedicated thread ..
        Executor iteratorThread = new ThreadPoolExecutor(0, 1,
                                    10L, TimeUnit.MILLISECONDS,
                                    new LinkedBlockingQueue<Runnable>());
        producer = new Subscription() {
            boolean complete = false;
            @Override
            public void request(long outern) {
                iteratorThread.execute(() -> {
                    if (complete) {
                        return;
                    }
                    long n = outern;
                    try {
                        while (iterator.hasNext() && n-- > 0) {
                            self().onNext(iterator.next());
                        }
                        if (!iterator.hasNext()) {
                            complete = true;
                            self().onComplete();
                        }
                    } catch (Throwable t) {
                        self().onError(t);
                    }
                });
            }

            @Override
            public void cancel() {
            }
        };
        processor = in -> (OUT)in;
        onSubscribe(producer);
        Thread.currentThread().setName(Thread.currentThread()+" (rx async stream processor)");
    }

    // with remoting a copy of subscriber is sent. calling "onSubscribe" on that
    // will do nothing and happen at remote side ;)
    // execute callerside
    // and break down remote communication to standard (auto remoted) primitives like promise and callback
    public ArrayList<Subscriber> _callerSideSubscribers; // advanced chemistry: held+used in the remote proxy to clean up pipeline on disconnect
    @Override @CallerSideMethod
    public void subscribe(Subscriber<? super OUT> subscriber) {
        if ( isRemote() ) {
            synchronized (this) { // subscribe/unsubscribe won't be contended
                if ( _callerSideSubscribers == null ) {
                    _callerSideSubscribers = new ArrayList();
                }
                _callerSideSubscribers.add(subscriber);
            }
        }
        if ( subscriber == null )
        {
            subscriber.onError( new IllegalArgumentException("cannot subscibe null") );
            return;
        }
        _subscribe((res, err) -> {
            if (isError(err)) {
                subscriber.onError((Throwable) err);
            } else if (isComplete(err)) {
                subscriber.onComplete();
            } else {
                subscriber.onNext((OUT) res);
            }
        }).then(subs -> {
            Log.Info(this, "stream subscribe acknowledged");
            subscriber.onSubscribe(subs);
        });
    }

    // private / as validation needs to be done synchronously, its on callerside. This is the async part of impl
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

    protected Subscription producer;
    protected long batchSize;
    protected long requestNextTrigger;
    protected long openRequested;

    public void setBatchSize(int batchSize ) {
        this.batchSize = batchSize;
        this.requestNextTrigger = batchSize/ KxReactiveStreams.REQU_NEXT_DIVISOR;
    }

    @Override @CallerSideMethod
    public void onSubscribe(Subscription subscription) {
        if ( subscription == null ) {
            throw null;
        }
        self()._onSubscribe(subscription);
    }

    // see onError() comment
    public void _onSubscribe(Subscription subscription) {
        producer = subscription;
        ArrayList<Runnable> tmp = this.doOnSubscribe;
        this.doOnSubscribe = null;
        tmp.forEach(runnable -> runnable.run());
    }

    protected void emitRequestNext() {
        if ( openRequested < requestNextTrigger ) {
            long minCredits = Long.MAX_VALUE;
            for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
                Entry<Integer, SubscriberEntry> next = iterator.next();
                long credits = next.getValue().credits;
                if ( minCredits > credits )
                    minCredits = credits;
                if ( credits < openRequested ) {
                    return;
                }
            }
            if ( isIteratorBased ) {
                if ( minCredits > 0) {
                    long min = Math.min(minCredits, batchSize);
                    producer.request(min);
                    openRequested += min;
                }
            } else {
                producer.request(batchSize);
                openRequested += batchSize;
            }
        }
    }

    // same as with onError
    @Override @CallerSideMethod public void onNext(IN in) {
        if ( in  == null )
            throw null;
        _onNext(in);
    }

    // see onError() comment
    public void _onNext(IN in) {
        if ( subscribers == null )
            return;
        openRequested--;
        try {
            OUT apply = processor.apply(in);
            if ( apply != null ) {
                forwardMessage(apply);
            } else {
                emitRequestNext();
            }
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
        subscribers.forEach((id, entry) -> {
            entry.onError(err);
        });
    }

    protected void forwardMessage(Object msg) {
        if (subscribers == null) {
            return;
        }

        if ( lossy ) {
            List toRemove = null;
            for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
                Entry<Integer, SubscriberEntry> next = iterator.next();
                SubscriberEntry entry = next.getValue();
                if (entry.getCredits() > 0) {
                    try {
                        entry.onNext(msg);
                    } catch (Throwable th) {
                        if ( toRemove == null ) {
                            toRemove = new ArrayList();
                        }
                        toRemove.add(entry);
                    }
                }
            }
            if ( toRemove != null ) {
                removeSubscribers(toRemove);
            }
            if ( subscribers.size() > 0 ) {
                emitRequestNext();
            }
        } else {
            long minCredits = calcMinCredits();

            if ( minCredits <= 0 ) {
                pending.addFirst(msg);
                return;
            }
            List toRemove = null;
            if ( pending.size() > 0 ) {
                pending.addFirst(msg);
                toRemove = forwardPending(minCredits, toRemove);
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
                removeSubscribers(toRemove);
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
    }

    protected long calcMinCredits() {
        long minCredits = Long.MAX_VALUE;
        for (Iterator<Entry<Integer, SubscriberEntry>> iterator = subscribers.entrySet().iterator(); iterator.hasNext(); ) {
            Entry<Integer, SubscriberEntry> next = iterator.next();
            SubscriberEntry entry = next.getValue();
            if ( minCredits > entry.getCredits() ) {
                minCredits = entry.getCredits();
            }
        }
        return minCredits;
    }

    protected void removeSubscribers(List toRemove) {
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

    protected List forwardPending(long minCredits, List toRemove) {
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
        return toRemove;
    }

    // callerside as spec expects error synchronously (wrong/inconsistent imo)
    @Override
    @CallerSideMethod public void onError(Throwable throwable) {
        if ( throwable == null )
            throw null;
        _onError(throwable);
    }

    // see comment onError()
    public void _onError(Throwable throwable) {
        forwardError(throwable);
        stop();
    }

    @Override
    public void onComplete() {
        if ( pending.size() > 0 ) {
//            System.out.println("pending loops ..");
            if ( pending.size() > 0 ) {
                List l = forwardPending(calcMinCredits(),null);
                if ( l != null && l.size() > 0 )
                    removeSubscribers(l);
            }
            delayed(1, () -> self().onComplete() );
        } else {
//            System.out.println("stopping "+__mailbox.size()+" "+__cbQueue.size() );
            subscribers.forEach((id, entry) -> {
                entry.onComplete();
            });
            stop();
        }
    }

    @Override // some client disconnected
    public void hasBeenUnpublished(String connectionIdentifier) {
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

    @Override
    public void stop() {
        if ( isPublished() ) {
            if ( closeOnComplete ) {
                ConcurrentLinkedQueue<RemoteConnection> connections = getConnections();
                close();
                for (Iterator<RemoteConnection> iterator = connections.iterator(); iterator.hasNext(); ) {
                    iterator.next().closeNetwork();
                }
            }
        }
        super.stop();
    }

    public void subscriberDisconnected(int id) {
        Log.Info(this, "a stream client disconnected id:" + id + " remaining:" + subscribers.size());
        emitRequestNext();
    }

    public void setCloseOnComplete(boolean closeOnComplete) {
        this.closeOnComplete = closeOnComplete;
    }


    public void setLossy(boolean lossy) {
        this.lossy = lossy;
    }

    @Override @CallerSideMethod
    public KxReactiveStreams getKxStreamsInstance() {
        if ( _streams == null ) {
            KxReactiveStreams streams = getActor()._streams;
            if ( streams == null ) {
                System.out.println("POK");
            }
            return streams;
        }
        if ( _streams == null ) {
            System.out.println("POK");
        }
        return _streams;
    }

    protected static class KSubscription implements Subscription, Serializable {

        protected KxPublisherActor publisher; // actorref
        protected int id;

        public KSubscription(KxPublisherActor publisher, int id) {
            this.publisher = publisher;
            this.id = id;
        }

        public int getId() {
            return id;
        }

        @Override
        public void request(long l) {
            if ( l <= 0 ) {
                publisher.onError(new IllegalArgumentException("spec rule 3.9: request > 0 elements"));
                return;
            }
            publisher._rq(l, id);
        }

        @Override
        public void cancel() {
            removeRegistration();
            publisher._cancel(id);
        }

        protected void removeRegistration() {
            if ( publisher._callerSideSubscribers != null ) {
                synchronized (publisher._callerSideSubscribers) {
                    publisher._callerSideSubscribers.remove(this);
                }
            }
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
            subscriber.pipe(msg);
            credits--;
        }

        public void onComplete() {
            subscriber.finish();
        }

    }

}
