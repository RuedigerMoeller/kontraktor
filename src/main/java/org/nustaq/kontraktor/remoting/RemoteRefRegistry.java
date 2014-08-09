package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 08.08.14.
 */
public class RemoteRefRegistry {

    RemoteScheduler scheduler = new RemoteScheduler(); // unstarted thread dummy
    FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

    // holds published actors of this process
    AtomicInteger actorIdCount = new AtomicInteger(0);
    ConcurrentHashMap<Integer, Actor> publishedActorMapping = new ConcurrentHashMap<>();
    ConcurrentHashMap<Actor, Integer> publishedActorMappingReverse = new ConcurrentHashMap<>();

    BackOffStrategy backOffStrategy = new BackOffStrategy();

    // have disabled dispacther thread
    ConcurrentLinkedQueue<Actor> remoteActors = new ConcurrentLinkedQueue<>();
    ConcurrentHashMap<Integer,Actor> remoteActorSet = new ConcurrentHashMap<>();

    public RemoteRefRegistry() {
        conf.registerSerializer(Actor.class,new ActorRefSerializer(this),true);
    }

    public int registerPublishedActor(Actor act) {
        Integer integer = publishedActorMappingReverse.get(act.getActorRef());
        if ( integer == null ) {
            integer = actorIdCount.incrementAndGet();
            publishedActorMapping.put(integer, act.getActorRef());
            publishedActorMappingReverse.put(act.getActorRef(), integer);
        }
        return integer;
    }

    public Actor getPublishedActor(int id) {
        return publishedActorMapping.get(id);
    }

    public Object readObjectFromStream(DataInputStream inputStream) throws Exception {
        int len = inputStream.readInt();
        byte buffer[] = new byte[len]; // this could be reused !
        while (len > 0)
            len -= inputStream.read(buffer, buffer.length - len, len);
        return conf.getObjectInput(buffer).readObject();
    }

    public void writeObjectToStream(DataOutputStream outputStream, Object toWrite) throws Exception {
        FSTObjectOutput objectOutput = conf.getObjectOutput(); // could also do new with minor perf impact
        objectOutput.writeObject(toWrite);
        outputStream.writeInt(objectOutput.getWritten());
        outputStream.write(objectOutput.getBuffer(), 0, objectOutput.getWritten());
        objectOutput.flush();
    }

    public RemoteScheduler getScheduler() {
        return scheduler;
    }

    public ConcurrentLinkedQueue<Actor> getRemoteActors() {
        return remoteActors;
    }

    public void registerRemoteRefDirect(Actor act) {
        act = act.getActorRef();
        remoteActorSet.put(act.__remoteId,act);
        remoteActors.add(act);
    }

    public Actor registerRemoteRef(Class actorClazz, int remoteId, Object client) {
        // fixme: requires client id in the key also
        Actor actorRef = remoteActorSet.get(remoteId);
        if ( actorRef == null ) {
            Actor res = Actors.AsActor(actorClazz, getScheduler());
            res.__remoteId = remoteId;
            res.__remotingImpl = this;
            remoteActorSet.put(remoteId,res);
            remoteActors.add(res);
            return res;
        }
        return actorRef;
    }

    protected void sendLoop(DataOutputStream outputStream) {
        int count = 0;
        while (true) {
            if ( singleSendLoop(outputStream) ) {
                count = 0;
            }
            backOffStrategy.yield(count++);
        }
    }

    protected void receiveLoop(DataInputStream inputStream) {
        try {
            while( true ) {
                // read object
                RemoteCallEntry read = (RemoteCallEntry) readObjectFromStream(inputStream);
                if ( this instanceof TCPActorClient ) {
                    System.out.println("POK");
                }
                if (read.getQueue() == read.MAILBOX) {
                    Actor targetActor = getPublishedActor(read.getReceiverKey());
                    targetActor.getScheduler().dispatchCall(null, targetActor,read.getMethod(),read.getArgs());
                } else if (read.getQueue() == read.CBQ) {
    //                    int count = 0;
    //                    while (!facade.__cbQueue.offer(read)) {
    //                        backOffStrategy.yield(count++);
    //                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * poll remote actor proxies and send. return true if there was at least one method
     * @param out
     */
    public boolean singleSendLoop(DataOutputStream out) {
        boolean res = false;
        for (Iterator<Actor> iterator = remoteActors.iterator(); iterator.hasNext(); ) {
            Actor remoteActor = iterator.next();
            try {
                CallEntry ce = (CallEntry) remoteActor.__cbQueue.poll();
                if ( ce != null ) {
                    RemoteCallEntry rce = new RemoteCallEntry(0, remoteActor.__remoteId, ce.getMethod().getName(), ce.getArgs());
                    rce.setQueue(rce.CBQ);
                    writeObjectToStream(out, rce);
                    res = true;
                }

                ce = (CallEntry) remoteActor.__mailbox.poll();
                if ( ce != null) {
                    RemoteCallEntry rce = new RemoteCallEntry(0, remoteActor.__remoteId,ce.getMethod().getName(),ce.getArgs());
                    rce.setQueue(rce.MAILBOX);
                    writeObjectToStream(out, rce);
                    res = true;
                }
            } catch (Exception ex) {
                System.out.println("connection closed");
                ex.printStackTrace();
                break;
            }
        }
        return res;
    }

}
