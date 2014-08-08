package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.impl.RemoteScheduler;
import org.nustaq.serialization.FSTConfiguration;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
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

    public RemoteRefRegistry() {
        conf.registerSerializer(Actor.class,new ActorRefSerializer(this),true);
    }

    public int getPublishedActorId(Actor act) {
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

    public Object readObjectFromStream(DataInputStream inputStream) throws IOException, ClassNotFoundException {
        int len = inputStream.readInt();
        byte buffer[] = new byte[len]; // this could be reused !
        while (len > 0)
            len -= inputStream.read(buffer, buffer.length - len, len);
        return conf.getObjectInput(buffer).readObject();
    }

    public void writeObjectToStream(DataOutputStream outputStream, Object toWrite) throws IOException {
        FSTObjectOutput objectOutput = conf.getObjectOutput(); // could also do new with minor perf impact
        objectOutput.writeObject(toWrite);
        outputStream.writeInt(objectOutput.getWritten());
        outputStream.write(objectOutput.getBuffer(), 0, objectOutput.getWritten());
        objectOutput.flush();
    }

    public RemoteScheduler getScheduler() {
        return scheduler;
    }

    public void registerRemoteRef(Actor res) {
        // remote ref needs to be polled by transport
    }
}
