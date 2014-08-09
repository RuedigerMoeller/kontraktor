package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.impl.CallEntry;
import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.kontraktor.remoting.tcp.TCPActorClient;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
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

    // holds published actors, futures and callbacks of this process
    AtomicInteger actorIdCount = new AtomicInteger(0);
    ConcurrentHashMap<Integer, Object> publishedActorMapping = new ConcurrentHashMap<>();
    ConcurrentHashMap<Object, Integer> publishedActorMappingReverse = new ConcurrentHashMap<>();

    BackOffStrategy backOffStrategy = new BackOffStrategy();

    // have disabled dispacther thread
    ConcurrentLinkedQueue<Actor> remoteActors = new ConcurrentLinkedQueue<>();
    ConcurrentHashMap<Integer,Actor> remoteActorSet = new ConcurrentHashMap<>();

    protected ThreadLocal<OutputStream> currentOutput = new ThreadLocal<>();

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

    public Object readObjectFromStream(InputStream inputStream) throws Exception {

        int ch1 = (inputStream.read() + 256) & 0xff;
        int ch2 = (inputStream.read()+ 256) & 0xff;
        int ch3 = (inputStream.read() + 256) & 0xff;
        int ch4 = (inputStream.read() + 256) & 0xff;
        int len = (ch4 << 24) + (ch3 << 16) + (ch2 << 8) + (ch1 << 0);

        byte buffer[] = new byte[len]; // this could be reused !
        while (len > 0)
            len -= inputStream.read(buffer, buffer.length - len, len);
        return conf.getObjectInput(buffer).readObject();
    }

    public void writeObjectToStream(OutputStream outputStream, Object toWrite) throws Exception {
        FSTObjectOutput objectOutput = conf.getObjectOutput(); // could also do new with minor perf impact
        objectOutput.writeObject(toWrite);

        int written = objectOutput.getWritten();
        outputStream.write((written >>> 0) & 0xFF);
        outputStream.write((written >>> 8) & 0xFF);
        outputStream.write((written >>> 16) & 0xFF);
        outputStream.write((written >>> 24) & 0xFF);

        outputStream.write(objectOutput.getBuffer(), 0, written);
        objectOutput.flush();
    }

    public RemoteScheduler getScheduler() {
        return scheduler;
    }

    public ConcurrentLinkedQueue<Actor> getRemoteActors() {
        return remoteActors;
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

    protected void sendLoop(OutputStream outputStream) {
        int count = 0;
        while (true) {
            if ( singleSendLoop(outputStream) ) {
                count = 0;
            }
            backOffStrategy.yield(count++);
        }
    }

    protected void receiveLoop(InputStream inputStream) {
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
                    Callback publishedCallback = getPublishedCallback(read.getReceiverKey());
                    publishedCallback.receiveResult(read.getArgs()[0],read.getArgs()[1]);
                    removePublishedObject(read.getReceiverKey());
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
    public boolean singleSendLoop(OutputStream out) {
        boolean res = false;
        for (Iterator<Actor> iterator = remoteActors.iterator(); iterator.hasNext(); ) {
            Actor remoteActor = iterator.next();
            try {
                CallEntry ce = null;
                // cbqueue unused at client side (direct cb)
//                ce = (CallEntry) remoteActor.__cbQueue.poll();
//                if ( ce != null ) {
//                    RemoteCallEntry rce = new RemoteCallEntry(0, remoteActor.__remoteId, ce.getMethod().getName(), ce.getArgs());
//                    rce.setQueue(rce.CBQ);
//                    writeObjectToStream(out, rce);
//                    res = true;
//                }

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

    public void receiveCBResult(OutputStream out, int id, Object result, Object error) throws Exception {
        RemoteCallEntry rce = new RemoteCallEntry(0, id, null, new Object[] {result,error});
        rce.setQueue(rce.CBQ);
        writeObjectToStream(out, rce);
    }
}
