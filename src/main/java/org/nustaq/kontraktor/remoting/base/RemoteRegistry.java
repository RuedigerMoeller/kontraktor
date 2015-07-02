/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

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

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.impl.InternalActorStoppedException;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.encoding.*;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.util.FSTUtil;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Created by moelrue on 5/7/15.
 *
 * Manages mapping of remote refs and callbacks. E.g. if an actor ref or callback or spore is sent to
 * a remote actor, during serialization RemoteRegistry generates and maps the callback id's required
 * to later on route incoming messages from remote to the appropriate local instances
 *
 */
public abstract class RemoteRegistry implements RemoteConnection {

    public static final Object OUT_OF_ORDER_SEQ = "OOOS";
    public static int MAX_BATCH_CALLS = 500;
    public static void registerDefaultClassMappings(FSTConfiguration conf) {
        conf.registerCrossPlatformClassMapping(new String[][]{
            {"call", RemoteCallEntry.class.getName()},
            {"cbw", CallbackWrapper.class.getName()}
        });
    }

    protected FSTConfiguration conf;
    protected RemoteScheduler scheduler = new RemoteScheduler(); // unstarted thread dummy
    // holds published actors, futures and callbacks of this process
    protected AtomicInteger actorIdCount = new AtomicInteger(0);
    protected ConcurrentHashMap<Integer, Object> publishedActorMapping = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Object, Integer> publishedActorMappingReverse = new ConcurrentHashMap<>();
    // have disabled dispacther thread
    protected ConcurrentLinkedQueue<Actor> remoteActors = new ConcurrentLinkedQueue<>();
    protected ConcurrentHashMap<Integer,Actor> remoteActorSet = new ConcurrentHashMap<>();
    protected volatile boolean terminated = false;
    protected BiFunction<Actor,String,Boolean> remoteCallInterceptor =
    (actor,methodName) -> {
        Method method = actor.__getCachedMethod(methodName, actor);
        if ( method == null || method.getAnnotation(Local.class) != null ) {
            return false;
        }
        return true;
    };
    protected Consumer<Actor> disconnectHandler;
    protected boolean isObsolete;
    private Actor facadeActor;

    public RemoteRegistry(FSTConfiguration conf, Coding coding) {
        this.conf = conf;
        configureSerialization(coding);
    }

    public RemoteRegistry(Coding code) {
		if ( code == null )
			code = new Coding(SerializerType.FSTSer);
	    conf = code.createConf();
        registerDefaultClassMappings(conf);
        configureSerialization(code);
	}

    public BiFunction<Actor, String, Boolean> getRemoteCallInterceptor() {
        return remoteCallInterceptor;
    }

    public void setRemoteCallInterceptor(BiFunction<Actor, String, Boolean> remoteCallInterceptor) {
        this.remoteCallInterceptor = remoteCallInterceptor;
    }

    protected void configureSerialization(Coding code) {
		conf.registerSerializer(Actor.class,new ActorRefSerializer(this),true);
		conf.registerSerializer(CallbackWrapper.class, new CallbackRefSerializer(this), true);
		conf.registerSerializer(Spore.class, new SporeRefSerializer(), true);
		conf.registerClass(RemoteCallEntry.class);
		conf.registerSerializer(Timeout.class, new TimeoutSerializer(), false);
	}

    public Actor getPublishedActor(int id) {
        return (Actor) publishedActorMapping.get(id);
    }

    public Callback getPublishedCallback(int id) {
        return (Callback) publishedActorMapping.get(id);
    }

    public RemoteScheduler getScheduler() {
        return scheduler;
    }

    public ConcurrentLinkedQueue<Actor> getRemoteActors() {
        return remoteActors;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public int publishActor(Actor act) {
        Integer integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer == null ) {
            integer = actorIdCount.incrementAndGet();
            publishActorDirect(integer, act);
        }
        return integer;
    }

    private void publishActorDirect(Integer integer, Actor act) {
        publishedActorMapping.put(integer, act.getActorRef());
        publishedActorMappingReverse.put(act.getActorRef(), integer);
        act.__addRemoteConnection(this);
    }

    /**
     * remove current <remoteId,actor> mappings if present.
     * return map containing removed mappings (for reconnection)
     *  @param act
     *
     */
    public void unpublishActor(Actor act) {
        Integer integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer != null ) {
            publishedActorMapping.remove(integer);
            publishedActorMappingReverse.remove(act.getActorRef());
            act.__removeRemoteConnection(this);
            if ( act instanceof RemotedActor) {
                ((RemotedActor) act).hasBeenUnpublished();
            }
        }
    }

    public int registerPublishedCallback(Callback cb) {
        Integer integer = publishedActorMappingReverse.get(cb);
        if ( integer == null ) {
            integer = actorIdCount.incrementAndGet();
            publishedActorMapping.put(integer, cb);
            publishedActorMappingReverse.put(cb, integer);
        }
        return integer;
    }

    public void removePublishedObject(int receiverKey) {
        Object remove = publishedActorMapping.remove(receiverKey);
        if ( remove != null ) {
            publishedActorMappingReverse.remove(remove);
        }
    }

    public void registerRemoteRefDirect(Actor act) {
        act = act.getActorRef();
        remoteActorSet.put(act.__remoteId,act);
        remoteActors.add(act);
        act.__clientConnection = this;
        act.__addStopHandler((actor, err) -> {
            remoteRefStopped((Actor) actor);
        });
    }

    public Actor registerRemoteActorRef(Class actorClazz, int remoteId, Object client) {
        Actor actorRef = remoteActorSet.get(remoteId);
        if ( actorRef == null ) {
            Actor res = Actors.AsActor(actorClazz, getScheduler());
            res.__remoteId = remoteId;
            remoteActorSet.put(remoteId,res);
            remoteActors.add(res);
            res.__addStopHandler((actor, err) -> {
                remoteRefStopped((Actor) actor);
            });
            res.__clientConnection = this;
            return res;
        }
        return actorRef;
    }

    /**
     * warning: MThreaded
     * @param actor
     */
    protected void remoteRefStopped(Actor actor) {
        removeRemoteActor(actor);
        actor.getActorRef().__stopped = true;
        actor.getActor().__stopped = true;
    }

    public void stopRemoteRefs() {
        new ArrayList<>(remoteActors).forEach((actor) -> {
            if (disconnectHandler != null)
                disconnectHandler.accept(actor);
            //don't call remoteRefStopped here as its designed to be overridden
            try {
                removeRemoteActor(actor);
            } catch (Exception e) {
                Log.Warn(this, e);
            }
            actor.getActorRef().__stopped = true;
            if (actor.getActor() != null)
                actor.getActor().__stopped = true;
        });
    }

    protected void removeRemoteActor(Actor act) {
        remoteActorSet.remove(act.__remoteId);
        remoteActors.remove(act);
        try {
            act.__stop();
        } catch (InternalActorStoppedException ase) {}
    }

    /**
     * process a remote call entry or an array of remote call entries.
     *
     * @param responseChannel - writer required to route callback messages
     * @param response
     * @param createdFutures - can be null. Contains futures created by the submitted callsequence
     * @return
     * @throws Exception
     */
    public boolean receiveObject(ObjectSocket responseChannel, ObjectSink receiver, Object response, List<IPromise> createdFutures) throws Exception {
        if ( response == RemoteRegistry.OUT_OF_ORDER_SEQ )
            return false;
        if ( response instanceof Object[] ) { // bundling. last element contains sequence
            Object arr[] = (Object[]) response;
            boolean hadResp = false;
            int max = arr.length - 1;
            int inSequence = 0;
            if (arr[max] instanceof Number == false) // no sequencing
                max++;
            else
                inSequence = ((Number) arr[max]).intValue();

            for (int i = 0; i < max; i++) {
                Object resp = arr[i];
                if (resp instanceof RemoteCallEntry == false) {
                    if ( resp != null && ! "SP".equals(resp) ) // FIXME: hack for short polling
                        Log.Lg.error(this, null, "unexpected response:" + resp); // fixme
                    hadResp = true;
                } else if (processRemoteCallEntry(responseChannel, (RemoteCallEntry) resp, createdFutures))
                    hadResp = true;
            }
            return hadResp;
        } else {
            if (response instanceof RemoteCallEntry == false) {
                if ( response != null && ! "SP".equals(response))
                    Log.Lg.error(this, null, "unexpected response:" + response); // fixme
                return true;
            }
            if (processRemoteCallEntry(responseChannel, (RemoteCallEntry) response, createdFutures)) return true;
        }
        return false;
    }

    protected boolean processRemoteCallEntry(ObjectSocket objSocket, RemoteCallEntry response, List<IPromise> createdFutures ) throws Exception {
        RemoteCallEntry read = response;
        boolean isContinue = read.getArgs().length > 1 && Callback.CONT.equals(read.getArgs()[1]);
        if ( isContinue )
            read.getArgs()[1] = Callback.CONT; // enable ==
        if (read.getQueue() == read.MAILBOX) {
            Actor targetActor = getPublishedActor(read.getReceiverKey());
            if (targetActor==null) {
                Log.Lg.error(this, null, "registry:"+System.identityHashCode(this)+" no actor found for key " + read);
                return true;
            }
            if (targetActor.isStopped() || targetActor.getScheduler() == null ) {
                Log.Lg.error(this, null, "actor found for key " + read + " is stopped and/or has no scheduler set");
                receiveCBResult(objSocket, read.getFutureKey(), null, new RuntimeException("Actor has been stopped"));
                return true;
            }
            if (remoteCallInterceptor != null && !remoteCallInterceptor.apply(targetActor,read.getMethod())) {
                Log.Warn(this,"remote message blocked by securityinterceptor "+targetActor.getClass().getName()+" "+read.getMethod());
                return false;
            }
            try {
                Object future = targetActor.getScheduler().enqueueCallFromRemote(this, null, targetActor, read.getMethod(), read.getArgs(), false);
                if ( future instanceof IPromise) {
                    Promise p = null;
                    if ( createdFutures != null ) {
                        p = new Promise();
                        createdFutures.add(p);
                    }
                    final Promise finalP = p;
                    Thread debug = Thread.currentThread();
                    ((IPromise) future).then( (r,e) -> {
                        try {
                            Thread debug1 = Thread.currentThread();
                            Thread debug2 = debug;
                            receiveCBResult(objSocket, read.getFutureKey(), r, e);
                            if ( finalP != null )
                                finalP.complete();
                        } catch (Exception ex) {
                            Log.Warn(this, ex, "");
                        }
                    });
                }
            } catch (Throwable th) {
                Log.Warn(this,th);
                if ( read.getFutureKey() > 0 ) {
                    receiveCBResult(objSocket, read.getFutureKey(), null, FSTUtil.toString(th));
                } else {
                    FSTUtil.<RuntimeException>rethrow(th);
                }
            }
        } else if (read.getQueue() == read.CBQ) {
            Callback publishedCallback = getPublishedCallback(read.getReceiverKey());
            if ( publishedCallback == null )
                throw new RuntimeException("Publisher already deregistered, set error to 'Actor.CONT' in order to signal more messages will be sent");
            publishedCallback.complete(read.getArgs()[0], read.getArgs()[1]); // is a wrapper enqueuing in caller
            if (!isContinue)
                removePublishedObject(read.getReceiverKey());
        }
        return createdFutures != null && createdFutures.size() > 0;
    }

    /**
     * cleanup after connection close
     */
    public void cleanUp() {
        conf.clearCaches();
        stopRemoteRefs();
        publishedActorMappingReverse.keySet().forEach((act) -> {
            if (act instanceof Actor)
                unpublishActor((Actor) act);
        });
        getFacadeProxy().__removeRemoteConnection(this);
    }

    protected void closeRef(CallEntry ce, ObjectSocket chan) throws IOException {
        if (ce.getTargetActor().getActorRef() == getFacadeProxy().getActorRef() ) {
            // invalidating connections should cleanup all refs
            chan.close();
        } else {
            removeRemoteActor(ce.getTargetActor());
        }
    }

    protected void writeObject(ObjectSocket chan, RemoteCallEntry rce) throws Exception {
        try {
            chan.writeObject(rce);
        } catch (Exception e) {
            setTerminated(true);
            cleanUp();
        }
    }

    public void receiveCBResult(ObjectSocket chan, int id, Object result, Object error) throws Exception {
        if (facadeActor!=null) {
            Thread debug = facadeActor.getCurrentDispatcher();
            if ( Thread.currentThread() != facadeActor.getCurrentDispatcher() ) {
                facadeActor.execute( () -> {
                    try {
                        if ( Thread.currentThread() != debug )
                            System.out.println("??");
                        receiveCBResult(chan,id,result, error);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return;
            }
        }
        if ( Callback.FINSILENT.equals(error) ) {
            return;
        }
        RemoteCallEntry rce = new RemoteCallEntry(0, id, null, new Object[] {result,error});
        rce.setQueue(rce.CBQ);
        writeObject(chan, rce);
    }

    public void close() {
        cleanUp();
    }

    public FSTConfiguration getConf() {
        return conf;
    }

    public abstract Actor getFacadeProxy();

    public void setDisconnectHandler(Consumer<Actor> disconnectHandler) {
        this.disconnectHandler = disconnectHandler;
    }

    public Consumer<Actor> getDisconnectHandler() {
        return disconnectHandler;
    }

    public void setClassLoader(ClassLoader l) {
        conf.setClassLoader(l);
    }

    public int getRemoteId(Actor act) {
        Integer integer = publishedActorMappingReverse.get(act.getActorRef());
        return integer == null ? -1 : integer;
    }

    /**
     * poll remote actor proxies and send. return true if there was at least one message
     * @param chanHolder
     */
    public boolean pollAndSend2Remote(AtomicReference<ObjectSocket> chanHolder) throws Exception {
        ObjectSocket chan = chanHolder.get();
        if ( chan == null || ! chan.canWrite() )
            return false;
        boolean hadAnyMsg = false;
        ArrayList<Actor> toRemove = null;
        int sumQueued;
        int fullqueued = 0;
        do {
            sumQueued = 0;
            for (Iterator<Actor> iterator = remoteActors.iterator(); iterator.hasNext(); ) {
                Actor remoteActor = iterator.next();
                boolean cb = false; // true; FIXME
                CallEntry ce = (CallEntry) remoteActor.__cbQueue.poll();
                if ( ce == null ) {
                    cb = false;
                    ce = (CallEntry) remoteActor.__mailbox.poll();
                }
                if ( ce != null) {
                    if ( ce.getMethod().getName().equals("close") ) {
                        closeRef(ce,chan);
                    } else
                    if ( ce.getMethod().getName().equals("asyncstop") ) {
                        Log.Lg.error(this, null, "cannot stop remote actors" );
                    } else {
                        int futId = 0;
                        if (ce.hasFutureResult()) {
                            futId = registerPublishedCallback(ce.getFutureCB());
                        }
                        try {
                            RemoteCallEntry rce = new RemoteCallEntry(futId, remoteActor.__remoteId, ce.getMethod().getName(), ce.getArgs());
                            rce.setQueue(cb ? rce.CBQ : rce.MAILBOX);
                            writeObject(chan, rce);
                            sumQueued++;
                            hadAnyMsg = true;
                        } catch (Exception ex) {
                            chan.setLastError(ex);
                            if (toRemove == null)
                                toRemove = new ArrayList();
                            toRemove.add(remoteActor);
                            remoteActor.__stop();
                            Log.Lg.infoLong(this, ex, "connection closed");
                            break;
                        }
                    }
                }
            }
            if (toRemove!=null) {
                toRemove.forEach( (act) -> removeRemoteActor(act) );
            }
            fullqueued += sumQueued;
        } while ( sumQueued > 0 && fullqueued < MAX_BATCH_CALLS);
        chan.flush();
        return hadAnyMsg;
    }

    public abstract AtomicReference<ObjectSocket> getWriteObjectSocket();

    public boolean isObsolete() {
        return isObsolete;
    }

    /**
     * give the application a way to explecitely flag a connection as obsolete
     *
     */
    public void setIsObsolete(boolean isObsolete) {
        this.isObsolete = isObsolete;
    }


    public int getRemoteActorSize() {
        return remoteActorSet.size();
    }

    public void setFacadeActor(Actor facadeActor) {
        this.facadeActor = facadeActor;
    }

    public Actor getFacadeActor() {
        return facadeActor;
    }
}
