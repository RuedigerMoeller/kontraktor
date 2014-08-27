package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.Spore;
import org.nustaq.serialization.*;

import java.io.IOException;
import java.lang.reflect.Field;

/**
 * Created by ruedi on 26.08.14.
 *
 * FIXME: pobably not needed anymore beacuse of @AnonymousTransient
 */
public class SporeRefSerializer extends FSTBasicObjectSerializer {

    @Override
    public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        in.defaultReadObject(referencedBy,clzInfo,toRead);
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        return super.instantiate(objectClass, in, serializationInfo, referencee, streamPositioin);
    }

    @Override
    public void writeObject(FSTObjectOutput out, Object toWrite, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy, int streamPosition) throws IOException {
        out.defaultWriteObject(toWrite, clzInfo);
    }

}
