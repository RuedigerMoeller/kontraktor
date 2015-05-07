package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * Created by ruedi on 08.08.14.
 */
public class ActorRefSerializer extends FSTBasicObjectSerializer {

    RemoteRegistry reg;

    public ActorRefSerializer(RemoteRegistry reg) {
        this.reg = reg;
    }

    @Override
    public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
    }

    @Override
    public boolean alwaysCopy() {
        return super.alwaysCopy();
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        // fixme: detect local actors returned from foreign
        int id = in.readInt();
        String clzName = in.readStringUTF();
        if (clzName.endsWith("_ActorProxy")) {
            clzName = clzName.substring(0,clzName.length()-"_ActorProxy".length());
        }
        Class actorClz = Class.forName(clzName,true,reg.getConf().getClassLoader());
        Actor actorRef = reg.registerRemoteActorRef(actorClz, id, null);
        in.registerObject(actorRef, streamPositioin, serializationInfo, referencee);
        return actorRef;
    }

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        // fixme: catch republish of foreign actor
        Actor act = (Actor) toWrite;
        int id = reg.publishActor(act); // register published host side FIXME: if ref is foreign ref, scnd id is required see javascript impl
        out.writeInt(id);
        out.writeStringUTF(act.getActorRef().getClass().getName());
    }
}
