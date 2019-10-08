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
import org.nustaq.kontraktor.annotations.RateLimited;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.annotations.Secured;
import org.nustaq.kontraktor.impl.*;
import org.nustaq.kontraktor.remoting.encoding.*;
import org.nustaq.kontraktor.routers.AbstractKrouter;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Created by moelrue on 5/7/15.
 *
 * Manages mapping of remote refs and callbacks of a single connection. E.g. if an actor ref or callback or spore is sent to
 * a remote actor, during serialization ConnectionRegistry generates and maps the callback id's required
 * to later on route incoming messages from remote to the appropriate local instances
 *
 */
public abstract class ConnectionRegistry {

    public static final Object OUT_OF_ORDER_SEQ = "OOOS";
    public static int MAX_BATCH_CALLS = 500;

    public static void registerDefaultClassMappings(FSTConfiguration conf) {
        conf.registerCrossPlatformClassMapping(new String[][]{
            {"call", RemoteCallEntry.class.getName()},
            {"cbw", CallbackWrapper.class.getName()}
        });
    }

    public static BiFunction remoteCallMapper; // if set, each remote call and callback is mapped through

    public AtomicReference<Object> userData = new AtomicReference<>();

    private ActorServer server;
    private boolean secured;

    protected long lastRoutingClientPing = 0; // only set if a client calls ping on a serving actor

    protected FSTConfiguration conf;
    protected RemoteScheduler scheduler = new RemoteScheduler(); // unstarted thread dummy
    // holds published actors, futures and callbacks of this process
    protected AtomicLong actorIdCount = new AtomicLong(0);
    protected ConcurrentHashMap<Long, Object> publishedActorMap = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Object, Long> publishedActorMappingReverse = new ConcurrentHashMap<>();
    // have disabled dispacther thread
    protected ConcurrentLinkedQueue<Actor> remoteActors = new ConcurrentLinkedQueue<>();
    protected ConcurrentHashMap<Long,Actor> remoteActorMap = new ConcurrentHashMap<>();
    protected volatile boolean terminated = false;
    protected Consumer<Actor> disconnectHandler;
    protected boolean isObsolete;
    protected Map<String,RateLimitEntry> rateLimits;
    private Actor facadeActor;
    protected BiFunction<Actor,String,Boolean> remoteCallInterceptor =
    (actor,methodName) -> {
        Method method = actor.__getCachedMethod(methodName, actor, null);
        if ( method == null ) {
            Log.Warn(null, "no such method on "+actor.getClass().getSimpleName()+"#"+methodName);
        }
        if ( method == null || ActorProxyFactory.getInheritedAnnotation(Local.class,method) != null ) {
            return false;
        }

        RateLimited rateLimited = ActorProxyFactory.getInheritedAnnotation(RateLimited.class, method);
        if ( rateLimited != null ) {
            synchronized (this) {
                if (rateLimits == null) {
                    rateLimits = new ConcurrentHashMap();
                }
                rateLimits.put(method.getName(), new RateLimitEntry(rateLimited));
            }
        }
        // fixme: this slows down remote call performance somewhat.
        // checks should be done before putting methods into cache
        if ( secured && ActorProxyFactory.getInheritedAnnotation(Remoted.class,method) == null ) {
            Log.Warn(null, "method not @Remoted "+actor.getClass().getSimpleName()+"#"+methodName);
            return false;
        }
        return true;
    };


    public ConnectionRegistry(FSTConfiguration conf, Coding coding) {
        this.conf = conf;
        configureSerialization(coding);
    }

    public ConnectionRegistry(Coding code) {
		if ( code == null )
			code = new Coding(SerializerType.FSTSer);
	    conf = code.createConf();
        registerDefaultClassMappings(conf);
        configureSerialization(code);
	}

    public BiFunction<Actor, String, Boolean> getRemoteCallInterceptor() {
        return remoteCallInterceptor;
    }

//    not applicable see actor.dispatchCall
//    public void setRemoteCallInterceptor(BiFunction<Actor, String, Boolean> remoteCallInterceptor) {
//        this.remoteCallInterceptor = remoteCallInterceptor;
//    }

    protected void configureSerialization(Coding code) {
		conf.registerSerializer(Actor.class,new ActorRefSerializer(this),true);
		conf.registerSerializer(CallbackWrapper.class, new CallbackRefSerializer(this), true);
		conf.registerSerializer(Spore.class, new SporeRefSerializer(), true);
        conf.registerSerializer(Timeout.class, new TimeoutSerializer(), false);
        conf.registerClass(RemoteCallEntry.class);
        conf.registerClass(Spore.class);
        conf.registerClass(CallbackWrapper.class);
        conf.registerClass(Actor.class);
	}

	public int getOpenRemoteMappingsCount() {
        return publishedActorMap.size();
    }

    public Actor getPublishedActor(long id) {
        return (Actor) publishedActorMap.get(id);
    }

    public Callback getPublishedCallback(long id) {
        Object o = publishedActorMap.get(id);
        if ( o instanceof Callback )
            return (Callback) o;
        return null;
    }

    public RemoteScheduler getScheduler() {
        return scheduler;
    }

    public ConcurrentLinkedQueue<Actor> getRemoteActors() {
        return remoteActors;
    }

    public ConcurrentHashMap<Long, Actor> getRemoteActorMap() {
        return remoteActorMap;
    }

    public boolean isTerminated() {
        return terminated;
    }

    public void setTerminated(boolean terminated) {
        this.terminated = terminated;
    }

    public long publishActor(Actor act) {
        Long integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer == null ) {
            integer = newActId();
            publishActorDirect(integer, act);
        }
        return integer;
    }

    private long newActId() {
        long id = actorIdCount.incrementAndGet();
        return id;
    }

    private void publishActorDirect(Long id, Actor act) {
        Object o = publishedActorMap.get(id);
        if ( o != null && o != act.getActorRef() ) {
            Log.Error(this,"id already present old:"+o+" new:"+act);
        }
        publishedActorMap.put(id, act.getActorRef());
        publishedActorMappingReverse.put(act.getActorRef(), id);
        act.__addRemoteConnection(this);
    }

    /**
     * remove current <remoteId,actor> mappings if present.
     * return map containing removed mappings (for reconnection)
     *  @param act
     *
     */
    public void unpublishActor(Actor act) {
        Long integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer != null ) {
            Log.Debug(this, ""+act.getClass().getSimpleName()+" unpublished");
            publishedActorMap.remove(integer);
            publishedActorMappingReverse.remove(act.getActorRef());
            act.__removeRemoteConnection(this);
            if ( act instanceof RemotedActor) {
                String connectionIdentifier = getSocketRef().getConnectionIdentifier();
                ((RemotedActor) act).hasBeenUnpublished(connectionIdentifier);
            }
        }
    }

    public long registerPublishedCallback(Callback cb) {
        Long integer = publishedActorMappingReverse.get(cb);
        if ( integer == null ) {
            integer = newActId();
            publishedActorMap.put(integer, cb);
            publishedActorMappingReverse.put(cb, integer);
        }
        return integer;
    }

    public void removePublishedObject(long receiverKey) {
        Object remove = publishedActorMap.remove(receiverKey);
        if ( remove != null ) {
            publishedActorMappingReverse.remove(remove);
        } else {
            Log.Warn(this,"MISS REMOVE:"+receiverKey);
        }
    }

    public void registerRemoteRefDirect(Actor act) {
        act = act.getActorRef();
        remoteActorMap.put(act.__remoteId,act);
        remoteActors.add(act);
        act.__clientConnection = this;
        act.__addStopHandler((actor, err) -> {
            remoteRefStopped((Actor) actor);
        });
    }

    public Actor registerRemoteActorRef(Class actorClazz, long remoteId, Object client) {
        Actor actorRef = remoteActorMap.get(remoteId);
        if ( actorRef == null ) {
            Actor res = Actors.AsActor(actorClazz, getScheduler());
            res.__remoteId = remoteId;
            remoteActorMap.put(remoteId,res);
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
            Actor tmp = actor.getActor();
            if (tmp != null)
                tmp.__stopped = true;
        });
    }

    protected void removeRemoteActor(Actor act) {
        remoteActorMap.remove(act.__remoteId);
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
     * @param authContext
     * @return
     * @throws Exception
     */
    public boolean receiveObject(ObjectSocket responseChannel, ObjectSink receiver, Object response, List<IPromise> createdFutures, Object authContext) throws Exception {
        if ( response == ConnectionRegistry.OUT_OF_ORDER_SEQ ) {
            Log.Warn(this,"out of sequence remote call received");
            return false;
        } else if ( response instanceof Reconnect && facadeActor instanceof SessionResurrector ) {
            String sid = ((Reconnect) response).getSessionId();
            Actor target = ((SessionResurrector) facadeActor).reanimate(sid,-1).await();
            if (target != null) {
                publishActorDirect(((Reconnect) response).getRemoteRefId(), target);
            }
            return false;
        } else
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
                } else {
                    try {
                        if (processRemoteCallEntry(responseChannel, (RemoteCallEntry) resp, createdFutures, authContext))
                            hadResp = true;
                    } catch (UnknownActorException uae) {
                        // FIXME: in wrong thread here. should enqueue a valid error response
                        Log.Warn(this,"Unknown actor id "+((RemoteCallEntry) resp).getReceiverKey());
                    }
                }
            }
            return hadResp;
        } else {
            if (response instanceof RemoteCallEntry == false) {
                if ( response != null && ! "SP".equals(response))
                    Log.Lg.error(this, null, "unexpected response:" + response); // fixme
                return true;
            }
            try {
                if (processRemoteCallEntry(responseChannel, (RemoteCallEntry) response, createdFutures, authContext)) return true;
            } catch (UnknownActorException uae) {
                inFacadeThread( () -> {
                    try {
                        responseChannel.writeObject("Unknown actor id "+((RemoteCallEntry) response).getReceiverKey());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return true;
            }
        }
        return false;
    }

    // dispatch incoming remotecalls, return true if a future has been created
    protected boolean processRemoteCallEntry(ObjectSocket objSocket, RemoteCallEntry response, List<IPromise> createdFutures, Object authContext) throws Exception {
        RemoteCallEntry read = response;
        long receiverKey = read.getReceiverKey();
        if (read.getQueue() == read.MAILBOX) {
            if ( remoteCallMapper != null ) {
                read = (RemoteCallEntry) remoteCallMapper.apply(this,read);
            }
            if ( facadeActor instanceof AbstractKrouter) {
                facadeActor.__dispatchRemoteCall(objSocket,read,this,createdFutures, authContext, remoteCallInterceptor, 0);
            } else {
                Actor targetActor = getPublishedActor(receiverKey);
                if (receiverKey < 0 && targetActor == null) // forward
                {
                    targetActor = getPublishedActor(-receiverKey);
                }
                if (targetActor == null) {
                    if (facadeActor instanceof SessionResurrector) {
                        try {
                            // FIXME: this could become a bottle neck as its synchronous
                            // workaround would be to create a queuing proxy until actor is returned
                            SessionResurrector actorRef = (SessionResurrector) facadeActor.getActorRef();
                            targetActor = actorRef.reanimate(objSocket.getConnectionIdentifier(), receiverKey).await();
                            if (targetActor != null) {
                                publishActorDirect(receiverKey, targetActor);
                            }
                        } catch (Throwable th) {
                            Log.Info(this, th);
                        }
                    }
                }
                if ( receiverKey == 0 ) // special: invoke client callback set on client's remoteproxy
                {
                    if ( getFacadeProxy() != null && getFacadeProxy().zzServerMsgCallback != null ) {
                        read.unpackArgs(conf);
                        getFacadeProxy().zzServerMsgCallback.complete(read,null);
                    }
                    return false;
                }
                if (targetActor == null) {
                    Log.Lg.error(this, null, "registry:" + System.identityHashCode(this) + " no actor found for key " + read);
                    throw new UnknownActorException("unknown actor id " + receiverKey);
                }
                long delay = 0;
                if ( rateLimits != null ) {
                    RateLimitEntry rateLimitEntry = rateLimits.get(read.getMethod());
                    if ( rateLimitEntry != null ) {
                        long now = System.currentTimeMillis();
                        delay = rateLimitEntry.registerCall(now, read.getMethod());
                    }
                }
                targetActor.__dispatchRemoteCall(objSocket, read, this, createdFutures, authContext, remoteCallInterceptor, delay);
            }
        } else if (read.getQueue() == read.CBQ) {
            if ( remoteCallMapper != null ) {
                read = (RemoteCallEntry) remoteCallMapper.apply(this,read);
            }
            Callback publishedCallback = getPublishedCallback(receiverKey);
            if ( publishedCallback == null ) {
                publishedCallback = getPublishedCallback(-receiverKey); // check forward
                if ( publishedCallback != null ) {
                    publishedCallback.complete(read,null); // in case of forwards => promote full remote call object
                    return false;
                }
                if ( read.getArgs() != null && read.getArgs().length == 2 && read.getArgs()[1] instanceof InternalActorStoppedException ) {
                    // FIXME: this might happen frequently as messages are in flight.
                    // FIXME: need a better way to handle this. Frequently it is not an error.
                    Log.Warn(this,"call to stopped remote actor");
                } else {
                    try {
                        read.unpackArgs(getConf());
                    } catch (Exception e) {
                        // quiet
                    }
                    Log.Warn(this, "Publisher already deregistered, set error to 'Actor.CONT' in order to signal more messages will be sent. " + read);
                }
            } else {
                boolean isContinue = read.isContinue();
                if ( publishedCallback instanceof CallbackWrapper && ((CallbackWrapper) publishedCallback).isRouted() ) {
                    publishedCallback.complete(read,null);
                } else {
                    read.unpackArgs(conf);
                    if (isContinue)
                        read.getArgs()[1] = Callback.CONT; // enable ==
                    publishedCallback.complete(read.getArgs()[0], read.getArgs()[1]); // is a wrapper enqueuing in caller
                }
                if (!isContinue)
                    removePublishedObject(receiverKey);
            }
        }
        return createdFutures != null && createdFutures.size() > 0;
    }

    /**
     * cleanup after (virtual) connection close
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

    /**
     * called from ObjectSocket in case of disconnect (decoding errors or network issues)
     */
    public void disconnect() {
        setTerminated(true);
        cleanUp();
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
            Log.Debug(this,"a connection closed '"+e.getMessage()+"', terminating registry");
            disconnect();
        }
    }

    public void inFacadeThread(Runnable toRun) {
        if (facadeActor!=null) {
            if ( Thread.currentThread() != facadeActor.getCurrentDispatcher() ) {
                facadeActor.execute( toRun );
                return;
            }
        } else {
            int debug = 1;
        }
        toRun.run();
    }

    public void forwardRemoteMessage(RemoteCallEntry rce) {
        try {
            ObjectSocket chan = getWriteObjectSocket().get();
            writeObject(chan, rce);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void receiveCBResult(ObjectSocket chan, long id, Object result, Object error) {
        RemoteCallEntry rce = new RemoteCallEntry(0, id, null, null, conf.asByteArray(new Object[] {result,error}));
        rce.setQueue(rce.CBQ);
        rce.setContinue( error == Actors.CONT );
        try {
            writeObject(chan, rce);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void close(Actor closedOne) {
        try {
            getWriteObjectSocket().get().flush();
            if ( closedOne.getActor() == facadeActor.getActor() ) {
                getWriteObjectSocket().get().close();
            }
        } catch (Exception e) {
            Log.Warn(this,e);
        }
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

    public long getRemoteId(Actor act) {
        Long integer = publishedActorMappingReverse.get(act.getActorRef());
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
                        long futId = 0;
                        if (ce.hasFutureResult()) {
                            futId = registerPublishedCallback(ce.getFutureCB());
                        }
                        try {
                            RemoteCallEntry rce = new RemoteCallEntry(futId, remoteActor.__remoteId, ce.getMethod().getName(), ce.getArgs(), null );
                            rce.setQueue(cb ? rce.CBQ : rce.MAILBOX);
                            rce.pack(conf);
                            writeObject(chan, rce);
                            sumQueued++;
                            hadAnyMsg = true;
                        } catch (Throwable ex) {
                            if ( ex instanceof InvocationTargetException && ((InvocationTargetException) ex).getTargetException() != null ) {
                                ex = ((InvocationTargetException) ex).getTargetException();
                            }
                            if ( ex instanceof IOError || ex instanceof IOException ) {
                                chan.setLastError(ex);
                                if (toRemove == null)
                                    toRemove = new ArrayList();
                                toRemove.add(remoteActor);
                                remoteActor.__stop();
                                Log.Lg.infoLong(this, ex, "connection closed");
                            } else {
                                Log.Error(this,ex);
                            }
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

    public ObjectSocket getSocketRef() {
        return getWriteObjectSocket().get();
    }

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
        return remoteActorMap.size();
    }

    public void setFacadeActor(Actor facadeActor) {
        this.facadeActor = facadeActor;
        if ( facadeActor.getActor().getClass().getAnnotation(Secured.class) != null) {
            secured = true;
        }
    }

    public Actor getFacadeActor() {
        return facadeActor;
    }

    public void setServer(ActorServer server) {
        this.server = server;
    }

    public ActorServer getServer() {
        return server;
    }

    public IPromise closeNetwork() {
        if ( server != null )
            return server.close();
        else {
            Log.Warn(null, "failed closing underlying network connection as server is null");
            return new Promise<>(null,"server is null");
        }
    }

    public void pingFromRoutingClient() {
        lastRoutingClientPing = System.currentTimeMillis();
    }

    public long getLastRoutingClientPing() {
        return lastRoutingClientPing;
    }

    public long[] getRemotedActorIds() {
        List<Long> res = new ArrayList<>();
        remoteActorMap.forEach( (id, actorref) -> {
            if ( actorref instanceof Actor ) {
                res.add(id);
            }
        });
        long lr[] = new long[res.size()];
        for (int i = 0; i < lr.length; i++) {
            lr[i] = res.get(i);
        }
        return lr;
    }

    public long[] getPublishedActorIds() {
        List<Long> res = new ArrayList<>();
        publishedActorMap.forEach( (id, actorref) -> {
            if ( actorref instanceof Actor ) {
                res.add(id);
            }
        });
        long lr[] = new long[res.size()];
        for (int i = 0; i < lr.length; i++) {
            lr[i] = res.get(i);
        }
        return lr;
    }

    public String getConnectionIdentifier() {
        ObjectSocket objectSocket = getWriteObjectSocket().get();
        if ( objectSocket != null ) {
            return objectSocket.getConnectionIdentifier();
        }
        return null;
    }
}
