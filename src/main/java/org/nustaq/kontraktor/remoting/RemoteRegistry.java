package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.impl.InternalActorStoppedException;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Created by moelrue on 5/7/15.
 */
public abstract class RemoteRegistry implements RemoteConnection {

    public static int MAX_BATCH_CALLS = 500;
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
    protected BiFunction<Actor,String,Boolean> remoteCallInterceptor;
    protected Consumer<Actor> disconnectHandler;
    protected boolean isObsolete;

	public RemoteRegistry(Coding code) {
		if ( code == null )
			code = new Coding(SerializerType.FSTSer);
	    switch (code.getCoding()) {
		    case MinBin:
			    conf = FSTConfiguration.createMinBinConfiguration();
			    break;
		    default:
			    conf = FSTConfiguration.createDefaultConfiguration();
	    }
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
        conf.registerCrossPlatformClassMapping(new String[][]{
                {"call", RemoteCallEntry.class.getName()},
                {"cbw", CallbackWrapper.class.getName()}
        });
		conf.registerSerializer(Timeout.class, new TimeoutSerializer(), false);
        if (code.getConfigurator()!=null) {
            code.getConfigurator().accept(conf);
        }
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
            if ( act instanceof RemotableActor) {
                ((RemotableActor) act).$hasBeenUnpublished();
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

    public boolean receiveObject(WriteObjectSocket channel, Object response) throws Exception {
        if ( response == RemoteRefRegistry.OUT_OF_ORDER_SEQ )
            return false;
        if ( response instanceof Object[] ) { // bundling. last element contains sequence
            Object arr[] = (Object[]) response;
            boolean hadResp = false;
            int max = arr.length - 1;
            if (arr[max] instanceof Number == false) // no sequencing
                max++;
            for (int i = 0; i < max; i++) {
                Object resp = arr[i];
                if (resp instanceof RemoteCallEntry == false) {
                    if ( resp != null )
                        Log.Lg.error(this, null, "unexpected response:" + resp); // fixme
                    hadResp = true;
                } else if (processRemoteCallEntry(channel, (RemoteCallEntry) resp))
                    hadResp = true;
            }
            return hadResp;
        } else {
            if (response instanceof RemoteCallEntry == false) {
                if ( response != null )
                    Log.Lg.error(this, null, "unexpected response:" + response); // fixme
                return true;
            }
            if (processRemoteCallEntry(channel, (RemoteCallEntry) response)) return true;
        }
        return false;
    }

    protected boolean processRemoteCallEntry(WriteObjectSocket channel, RemoteCallEntry response) throws Exception {
        RemoteCallEntry read = response;
        boolean isContinue = read.getArgs().length > 1 && Callback.CONT.equals(read.getArgs()[1]);
        if ( isContinue )
            read.getArgs()[1] = Callback.CONT; // enable ==
        if (read.getQueue() == read.MAILBOX) {
            Actor targetActor = getPublishedActor(read.getReceiverKey());
            if (targetActor==null) {
                Log.Lg.error(this, null, "no actor found for key " + read);
                return true;
            }
            if (targetActor.isStopped() || targetActor.getScheduler() == null ) {
                Log.Lg.error(this, null, "actor found for key " + read + " is stopped and/or has no scheduler set");
                receiveCBResult(channel, read.getFutureKey(), null, new RuntimeException("Actor has been stopped"));
                return true;
            }
            if (remoteCallInterceptor != null && !remoteCallInterceptor.apply(targetActor,read.getMethod())) {
                Log.Warn(this,"remote message blocked by securityinterceptor "+targetActor.getClass().getName()+" "+read.getMethod());
                if ( read.getFutureKey() > 0 ) {

                }
                return true;
            }

            Object future = targetActor.getScheduler().enqueueCallFromRemote(this, null, targetActor, read.getMethod(), read.getArgs(), false);
            if ( future instanceof IPromise) {
                ((IPromise) future).then( (r,e) -> {
                    try {
                        receiveCBResult(channel, read.getFutureKey(), r, e);
                    } catch (Exception ex) {
                        Log.Warn(this, ex, "");
                    }
                });
            }
        } else if (read.getQueue() == read.CBQ) {
            Callback publishedCallback = getPublishedCallback(read.getReceiverKey());
            if ( publishedCallback == null )
                throw new RuntimeException("Publisher already deregistered, set error to 'Actor.CONT' in order to signal more messages will be sent");
            publishedCallback.complete(read.getArgs()[0], read.getArgs()[1]); // is a wrapper enqueuing in caller
            if (!isContinue)
                removePublishedObject(read.getReceiverKey());
        }
        return false;
    }

    /**
     * cleanup after connection close
     */
    public void cleanUp() {
        stopRemoteRefs();
        publishedActorMappingReverse.keySet().forEach((act) -> {
            if (act instanceof Actor)
                unpublishActor((Actor) act);
        });
        getFacadeProxy().__removeRemoteConnection(this);
    }

    protected void closeRef(CallEntry ce, WriteObjectSocket chan) throws IOException {
        if (ce.getTargetActor().getActorRef() == getFacadeProxy().getActorRef() ) {
            // invalidating connections should cleanup all refs
            chan.close();
        } else {
            removeRemoteActor(ce.getTargetActor());
        }
    }

    protected void writeObject(WriteObjectSocket chan, RemoteCallEntry rce) throws Exception {
        chan.writeObject(rce);
    }

    public void receiveCBResult(WriteObjectSocket chan, int id, Object result, Object error) throws Exception {
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
     * @param chan
     */
    public boolean pollAndSend2Remote(WriteObjectSocket chan) throws Exception {
        if ( chan == null )
            return false;
        boolean hadAnyMsg = false;
        ArrayList<Actor> toRemove = null;
        int sumQueued;
        int fullqueued = 0;
        do {
            sumQueued = 0;
            for (Iterator<Actor> iterator = remoteActors.iterator(); iterator.hasNext(); ) {
                Actor remoteActor = iterator.next();
                CallEntry ce = (CallEntry) remoteActor.__mailbox.poll();
                if ( ce != null) {
                    if ( ce.getMethod().getName().equals("$close") ) {
                        closeRef(ce,chan);
                    } else
                    if ( ce.getMethod().getName().equals("async$stop") ) {
                        Log.Lg.error(this, null, "cannot stop remote actors" );
                    } else {
                        int futId = 0;
                        if (ce.hasFutureResult()) {
                            futId = registerPublishedCallback(ce.getFutureCB());
                        }
                        try {
                            RemoteCallEntry rce = new RemoteCallEntry(futId, remoteActor.__remoteId, ce.getMethod().getName(), ce.getArgs());
                            rce.setQueue(rce.MAILBOX);
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

    public abstract AtomicReference<WriteObjectSocket> getWriteObjectSocket();

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

}
