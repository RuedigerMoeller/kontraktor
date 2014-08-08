package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * Created by ruedi on 08.08.14.
 */
public class ActorRefSerializer extends FSTBasicObjectSerializer {

    RemoteRefRegistry reg;

    public ActorRefSerializer(RemoteRefRegistry reg) {
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
        Class actorClz = in.readClass().getClazz();
        Actor res = Actors.AsActor(actorClz, reg.getScheduler() );
        res.__remoteId = id;
        res.__remotingImpl = reg;
        in.registerObject(res,streamPositioin,serializationInfo,referencee);
        reg.registerRemoteRef(res);
        return res;
    }

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        // fixme: catch republish of foreign actor
        Actor act = (Actor) toWrite;
        int id = reg.getPublishedActorId(act); // register published host side
        out.writeInt(id);
        out.writeClassTag(act.getActor().getClass());
    }
}
