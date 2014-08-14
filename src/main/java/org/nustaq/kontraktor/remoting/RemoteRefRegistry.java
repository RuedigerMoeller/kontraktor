package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 08.08.14.
 */
public class RemoteRefRegistry {

    protected FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    RemoteScheduler scheduler = new RemoteScheduler(); // unstarted thread dummy

    // holds published actors, futures and callbacks of this process
    AtomicInteger actorIdCount = new AtomicInteger(0);
    ConcurrentHashMap<Integer, Object> publishedActorMapping = new ConcurrentHashMap<>();
    ConcurrentHashMap<Object, Integer> publishedActorMappingReverse = new ConcurrentHashMap<>();

    BackOffStrategy backOffStrategy = new BackOffStrategy();

    // have disabled dispacther thread
    ConcurrentLinkedQueue<Actor> remoteActors = new ConcurrentLinkedQueue<>();
    ConcurrentHashMap<Integer,Actor> remoteActorSet = new ConcurrentHashMap<>();

    protected ThreadLocal<ObjectSocket> currentChannel = new ThreadLocal<>();

    public RemoteRefRegistry() {
        conf.registerSerializer(Actor.class,new ActorRefSerializer(this),true);
        conf.registerSerializer(CallbackWrapper.class, new CallbackRefSerializer(this), true);
        conf.registerClass(RemoteCallEntry.class);
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

    public int publishActor(Actor act) {
        Integer integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer == null ) {
            integer = actorIdCount.incrementAndGet();
            publishedActorMapping.put(integer, act.getActorRef());
            publishedActorMappingReverse.put(act.getActorRef(), integer);
        }
        return integer;
    }

    public void unpublishActor(Actor act) {
        Integer integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer != null ) {
            publishedActorMapping.remove(integer);
            publishedActorMappingReverse.remove(act.getActorRef());
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
    }

    public Actor registerRemoteActorRef(Class actorClazz, int remoteId, Object client) {
        Actor actorRef = remoteActorSet.get(remoteId);
        if ( actorRef == null ) {
            Actor res = Actors.AsActor(actorClazz, getScheduler());
            res.__remoteId = remoteId;
            remoteActorSet.put(remoteId,res);
            remoteActors.add(res);
            return res;
        }
        return actorRef;
    }

    private void removeRemoteActor(Actor act) {
        remoteActorSet.remove(act.__remoteId);
        remoteActors.add(act);
    }

    protected void sendLoop(ObjectSocket channel) throws IOException {
        int count = 0;
        while (true) {
            if ( singleSendLoop(channel) ) {
                count = 0;
            }
            backOffStrategy.yield(count++);
        }
    }

    protected void receiveLoop(ObjectSocket channel) {
        try {
            while( true ) {
                // read object
                RemoteCallEntry read = (RemoteCallEntry) channel.readObject();
                if (read.getQueue() == read.MAILBOX) {
                    Actor targetActor = getPublishedActor(read.getReceiverKey());
                    if (targetActor==null) {
                        System.out.println("no actor found for key "+read);
                        continue;
                    }
                    Object future = targetActor.getScheduler().enqueueCall(null, targetActor, read.getMethod(), read.getArgs());
                    if ( future instanceof Future ) {
                        ((Future) future).then( (r,e) -> {
                            try {
                                receiveCBResult(channel, read.getFutureKey(), r, e);
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        });
                    }
                } else if (read.getQueue() == read.CBQ) {
                    Callback publishedCallback = getPublishedCallback(read.getReceiverKey());
                    publishedCallback.receiveResult(read.getArgs()[0],read.getArgs()[1]); // is a wrapper enqueuing in caller
                    removePublishedObject(read.getReceiverKey());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * poll remote actor proxies and send. return true if there was at least one method
     * @param chan
     */
    public boolean singleSendLoop(ObjectSocket chan) throws IOException {
        if ( this instanceof TCPActorClient ) {
            int y = 99;
        }
        boolean res = false;
        int sumQueued = 0;
        ArrayList<Actor> toRemove = null;
        for (Iterator<Actor> iterator = remoteActors.iterator(); iterator.hasNext(); ) {
            Actor remoteActor = iterator.next();
                CallEntry ce = (CallEntry) remoteActor.__mailbox.poll();
                if ( ce != null) {
                    sumQueued += remoteActor.__mailbox.size();
                    int futId = 0;
                    if ( ce.hasFutureResult() ) {
                        futId = registerPublishedCallback(ce.getFutureCB());
                    }
                    try {
                        RemoteCallEntry rce = new RemoteCallEntry(futId, remoteActor.__remoteId,ce.getMethod().getName(),ce.getArgs());
                        rce.setQueue(rce.MAILBOX);
                        chan.writeObject(rce);
                        res = true;
                    } catch (Exception ex) {
                        chan.setLastError(ex);
                        if (toRemove==null)
                            toRemove = new ArrayList();
                        toRemove.add(remoteActor);
                        remoteActor.$stop();
                        System.out.println("connection closed");
                        ex.printStackTrace();
                        break;
                    }
                }
        }
        if (toRemove!=null) {
            toRemove.forEach( (act) -> removeRemoteActor(act) );
        }
        if ( sumQueued < 100 )
        {
            chan.flush();
        }
        return res;
    }

    public void receiveCBResult(ObjectSocket chan, int id, Object result, Object error) throws Exception {
        RemoteCallEntry rce = new RemoteCallEntry(0, id, null, new Object[] {result,error});
        rce.setQueue(rce.CBQ);
        chan.writeObject(rce);
    }
}
