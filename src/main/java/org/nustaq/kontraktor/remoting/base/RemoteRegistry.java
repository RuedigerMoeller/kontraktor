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

import java.io.IOError;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicLong;
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
    private ActorServer server;

    public static void registerDefaultClassMappings(FSTConfiguration conf) {
        conf.registerCrossPlatformClassMapping(new String[][]{
            {"call", RemoteCallEntry.class.getName()},
            {"cbw", CallbackWrapper.class.getName()}
        });
    }

    public static BiFunction remoteCallMapper; // if set, each remote call and callback is mapped through

    protected FSTConfiguration conf;
    protected RemoteScheduler scheduler = new RemoteScheduler(); // unstarted thread dummy
    // holds published actors, futures and callbacks of this process
    protected AtomicLong actorIdCount = new AtomicLong(0);
    protected ConcurrentHashMap<Long, Object> publishedActorMapping = new ConcurrentHashMap<>();
    protected ConcurrentHashMap<Object, Long> publishedActorMappingReverse = new ConcurrentHashMap<>();
    // have disabled dispacther thread
    protected ConcurrentLinkedQueue<Actor> remoteActors = new ConcurrentLinkedQueue<>();
    protected ConcurrentHashMap<Long,Actor> remoteActorSet = new ConcurrentHashMap<>();
    protected volatile boolean terminated = false;
    protected BiFunction<Actor,String,Boolean> remoteCallInterceptor =
    (actor,methodName) -> {
        Method method = actor.__getCachedMethod(methodName, actor);
        if ( method == null ) {
            Log.Warn(null, "no such method on "+actor.getClass().getSimpleName()+"#"+methodName);
        }
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
        conf.registerSerializer(Timeout.class, new TimeoutSerializer(), false);
        conf.registerClass(RemoteCallEntry.class);
        conf.registerClass(Spore.class);
        conf.registerClass(CallbackWrapper.class);
        conf.registerClass(Actor.class);
	}

    public Actor getPublishedActor(long id) {
        return (Actor) publishedActorMapping.get(id);
    }

    public Callback getPublishedCallback(long id) {
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
        Object o = publishedActorMapping.get(id);
        if ( o != null && o != act.getActorRef() ) {
            Log.Error(this,"id already present old:"+o+" new:"+act);
        }
        publishedActorMapping.put(id, act.getActorRef());
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
            publishedActorMapping.remove(integer);
            publishedActorMappingReverse.remove(act.getActorRef());
            act.__removeRemoteConnection(this);
            if ( act instanceof RemotedActor) {
                ((RemotedActor) act).hasBeenUnpublished();
            }
        }
    }

    public long registerPublishedCallback(Callback cb) {
        Long integer = publishedActorMappingReverse.get(cb);
        if ( integer == null ) {
            integer = newActId();
            publishedActorMapping.put(integer, cb);
            publishedActorMappingReverse.put(cb, integer);
        }
        return integer;
    }

    public void removePublishedObject(long receiverKey) {
        Object remove = publishedActorMapping.remove(receiverKey);
        if ( remove != null ) {
            publishedActorMappingReverse.remove(remove);
//            System.out.println("CBMAP SIZE:"+publishedActorMapping.size());
            int debug = 1;
        } else {
            Log.Warn(this,"MISS REMOVE:"+receiverKey);
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

    public Actor registerRemoteActorRef(Class actorClazz, long remoteId, Object client) {
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
            Actor tmp = actor.getActor();
            if (tmp != null)
                tmp.__stopped = true;
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
     * @param authContext
     * @return
     * @throws Exception
     */
    public boolean receiveObject(ObjectSocket responseChannel, ObjectSink receiver, Object response, List<IPromise> createdFutures, Object authContext) throws Exception {
        if ( response == RemoteRegistry.OUT_OF_ORDER_SEQ ) {
            Log.Warn(this,"out of sequence remote call received");
            return false;
        }
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
                } else if (processRemoteCallEntry(responseChannel, (RemoteCallEntry) resp, createdFutures, authContext))
                    hadResp = true;
            }
            return hadResp;
        } else {
            if (response instanceof RemoteCallEntry == false) {
                if ( response != null && ! "SP".equals(response))
                    Log.Lg.error(this, null, "unexpected response:" + response); // fixme
                return true;
            }
            if (processRemoteCallEntry(responseChannel, (RemoteCallEntry) response, createdFutures, authContext)) return true;
        }
        return false;
    }

    // dispatch incoming remotecalls, return true if a future has been created
    protected boolean processRemoteCallEntry(ObjectSocket objSocket, RemoteCallEntry response, List<IPromise> createdFutures, Object authContext) throws Exception {
        RemoteCallEntry read = response;
        if (read.getQueue() == read.MAILBOX) {
            if ( remoteCallMapper != null ) {
                read = (RemoteCallEntry) remoteCallMapper.apply(this,read);
            }
            Actor targetActor = getPublishedActor(read.getReceiverKey());
            if (targetActor==null) {
                if ( facadeActor instanceof SessionResurrector ) {
                    try {
                        // note this could become a bottle neck as its synchronous
                        // workaround would be to create a queuing proxy until actor is returned
                        targetActor = ((SessionResurrector) facadeActor.getActorRef()).reanimate(objSocket.getConnectionIdentifier(), read.getReceiverKey()).await();
                        if (targetActor != null) {
                            publishActorDirect(read.getReceiverKey(), targetActor);
                        }
                    } catch (Throwable th) {
                        Log.Info(this,th);
                    }
                }
            }
            if (targetActor==null) {
                Log.Lg.error(this, null, "registry:"+System.identityHashCode(this)+" no actor found for key " + read);
                return true;
            }
            targetActor.__dispatchRemoteCall(objSocket,read,this,createdFutures);
        } else if (read.getQueue() == read.CBQ) {
            if ( remoteCallMapper != null ) {
                read = (RemoteCallEntry) remoteCallMapper.apply(this,read);
            }
            Callback publishedCallback = getPublishedCallback(read.getReceiverKey());
            if ( publishedCallback == null ) {
                publishedCallback = getPublishedCallback(-read.getReceiverKey()); // check forward
                if ( publishedCallback != null ) {
                    publishedCallback.complete(read,null); // in case of forwards => promote full remote call object
                    return false;
                }
                if ( read.getArgs() != null && read.getArgs().length == 2 && read.getArgs()[1] instanceof InternalActorStoppedException ) {
                    // FIXME: this might happen frequently as messages are in flight.
                    // FIXME: need a better way to handle this. Frequently it is not an error.
                    Log.Warn(this,"call to stopped remote actor");
                } else
                    Log.Warn(this,"Publisher already deregistered, set error to 'Actor.CONT' in order to signal more messages will be sent. "+read);
            } else {
                boolean isContinue = read.isContinue();
                read.unpackArgs(conf);
                if ( isContinue )
                    read.getArgs()[1] = Callback.CONT; // enable ==
                publishedCallback.complete(read.getArgs()[0], read.getArgs()[1]); // is a wrapper enqueuing in caller
                if (!isContinue)
                    removePublishedObject(read.getReceiverKey());
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

    public void receiveCBResult(ObjectSocket chan, long id, Object result, Object error) throws Exception {
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
        RemoteCallEntry rce = new RemoteCallEntry(0, id, null, null, conf.asByteArray(new Object[] {result,error}));
        rce.setQueue(rce.CBQ);
        rce.setContinue( error == Actors.CONT );
        writeObject(chan, rce);
    }

    public void close() {
        try {
            getWriteObjectSocket().get().flush();
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

    //WARNING: must be called on correct thread ! FIXME: batching ??
    public void forwardRemoteMessage(RemoteCallEntry rce) {
        try {
            ObjectSocket chan = getWriteObjectSocket().get();
            writeObject(chan, rce);
            chan.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public abstract AtomicReference<ObjectSocket> getWriteObjectSocket();

    @Override
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
        return remoteActorSet.size();
    }

    public void setFacadeActor(Actor facadeActor) {
        this.facadeActor = facadeActor;
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

    @Override
    public IPromise closeNetwork() {
        if ( server != null )
            return server.close();
        else {
            Log.Warn(null, "failed closing underlying network connection as server is null");
            return new Promise<>(null,"server is null");
        }
    }
}
