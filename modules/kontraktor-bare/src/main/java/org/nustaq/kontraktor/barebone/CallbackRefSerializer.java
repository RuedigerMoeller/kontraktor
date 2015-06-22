/*
 * Copyright 2014 Ruediger Moeller.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.nustaq.kontraktor.barebone;

import org.nustaq.serialization.FSTBasicObjectSerializer;
import org.nustaq.serialization.FSTClazzInfo;
import org.nustaq.serialization.FSTObjectInput;
import org.nustaq.serialization.FSTObjectOutput;

import java.io.IOException;

/**
 * Created by ruedi on 09.08.14.
 */
public class CallbackRefSerializer extends FSTBasicObjectSerializer {

    RemoteActorConnection reg;

    public CallbackRefSerializer(RemoteActorConnection reg) {
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
        // fixme: detect local actors returned from foreign
        int id = in.readInt();
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
        int id = reg.registerCallback((Callback) toWrite); // register published host side
        out.writeInt(id);
    }

}
