package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * Created by ruedi on 09.08.14.
 */
public class CallbackRefSerializer extends FSTBasicObjectSerializer {

    RemoteRefRegistry reg;

    public CallbackRefSerializer(RemoteRefRegistry reg) {
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
        ObjectSocket chan = reg.currentChannel.get();
        Callback cb = (Object result, Object error) -> {
            try {
                reg.receiveCBResult(chan,id,result,error);
            } catch (Exception e) {
                Log.Warn(this, e, "");
            }
        };
        in.registerObject(cb, streamPositioin, serializationInfo, referencee);
        return cb;
    }

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        // fixme: catch republish of foreign actor
        int id = reg.registerPublishedCallback((Callback) toWrite); // register published host side
        out.writeInt(id);
    }

}
