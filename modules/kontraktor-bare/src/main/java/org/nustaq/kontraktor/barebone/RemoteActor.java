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

/**
 * Created by ruedi on 05/06/15.
 */
public class RemoteActor {
    protected int remoteId;
    protected RemoteActorConnection con;
    String name;

    public RemoteActor(String name, int remoteId, RemoteActorConnection con) {
        this.remoteId = remoteId;
        this.con = con;
        this.name = name;
    }

    public void tell( String methodName, Object ... arguments ) {
        con.addRequest(new RemoteCallEntry(remoteId,0,methodName,arguments,0), null);
    }

    public Promise ask( String methodName, Object ... arguments ) {
        Promise res = new Promise<>();
        con.addRequest(new RemoteCallEntry(remoteId,0,methodName,arguments,0),res);
        return res;
    }

    public int getRemoteId() {
        return remoteId;
    }

    public RemoteActorConnection getCon() {
        return con;
    }

    @Override
    public String toString() {
        return "RemoteActor{" +
                   "remoteId=" + remoteId +
                   ", con=" + con +
                   ", name='" + name + '\'' +
                   '}';
    }
}
