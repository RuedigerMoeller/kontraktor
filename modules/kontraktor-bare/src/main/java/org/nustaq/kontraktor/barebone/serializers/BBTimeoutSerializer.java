package org.nustaq.kontraktor.barebone.serializers;

import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * Created by ruedi on 08.11.14.
 */
public class BBTimeoutSerializer extends FSTBasicObjectSerializer {

    public static final RemoteActor._Timeout TIMEOUT = new RemoteActor._Timeout();

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        out.defaultWriteObject(toWrite,clzInfo);
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return TIMEOUT;
    }

}
