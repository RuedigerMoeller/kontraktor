/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
*/

package org.nustaq.kontraktor.remoting.encoding;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
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
    public void readObject(FSTObjectInput in, Object toRead, FSTClazzInfo clzInfo, FSTClazzInfo.FSTFieldInfo referencedBy) throws Exception {
    }

    @Override
    public boolean alwaysCopy() {
        return super.alwaysCopy();
    }

    @Override
    public Object instantiate(Class objectClass, FSTObjectInput in, FSTClazzInfo serializationInfo, FSTClazzInfo.FSTFieldInfo referencee, int streamPositioin) throws Exception {
        // fixme: detect local actors returned from foreign
        long id = in.readLong();
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
        long id = reg.publishActor(act); // register published host side FIXME: if ref is foreign ref, scnd id is required see javascript impl
        out.writeLong(id);
        out.writeStringUTF(act.getActorRef().getClass().getName());
    }
}
