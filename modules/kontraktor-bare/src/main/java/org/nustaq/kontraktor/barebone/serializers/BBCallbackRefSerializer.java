package org.nustaq.kontraktor.barebone.serializers;

import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * Created by ruedi on 09.08.14.
 */
public class BBCallbackRefSerializer extends FSTBasicObjectSerializer {

    RemoteActor reg;

    public BBCallbackRefSerializer(RemoteActor reg) {
        this.reg = reg;
    }

    @Override
    public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
    }

    @Override
    public boolean alwaysCopy() {
        return super.alwaysCopy();
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws Exception {
//        int id = in.readInt();
//        AtomicReference<ObjectSocket> chan = reg.getWriteObjectSocket();
//        Callback cb = (Object result, Object error) -> {
//            try {
//                reg.receiveCBResult(chan.get(),id,result,error);
//            } catch (Exception e) {
//                Log.Warn(this, e, "");
//            }
//        };
//        in.registerObject(cb, streamPositioin, serializationInfo, referencee);
//        return cb;
        return null;
    }

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
//        int id = reg.registerPublishedCallback((Callback) toWrite); // register published host side
//        out.writeInt(id);
    }

}
