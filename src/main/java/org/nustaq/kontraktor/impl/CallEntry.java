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

package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ConnectionRegistry;

import java.lang.reflect.Method;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry<T> {

    final private Method method;
    final private Object[] args;
    private IPromise futureCB;
    final private T target;    // target and target actor are not necessary equal. E.g. target can be callback, but calls are put onto sendingActor Q
    private Actor sendingActor; // defines the sender of this message. null in case of outside call
    private Actor targetActor;  // defines actor assignment in case target is callback
    private boolean onCBQueue;  // determines queue used
    private ConnectionRegistry remoteRefRegistry; // remote connection call came from
    private int trackingId;

    public CallEntry(T target, Method method, Object[] args, Actor sender, Actor targetActor, boolean isCB) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.sendingActor = sender;
        this.targetActor = targetActor;
        this.onCBQueue = isCB;
    }

    public void setRemoteRefRegistry(ConnectionRegistry remoteRefRegistry) {
        this.remoteRefRegistry = remoteRefRegistry;
    }

    public ConnectionRegistry getRemoteRefRegistry() {
        return remoteRefRegistry;
    }

    public Actor getTargetActor() {
        return targetActor;
    }
    public T getTarget() {
        return target;
    }
    public Method getMethod() {
        return method;
    }
    public Object[] getArgs() { return args; }

    public Actor getSendingActor() {
        return sendingActor;
    }

    public boolean hasFutureResult() {
        return method.getReturnType() == IPromise.class;
    }

    public void setFutureCB(IPromise futureCB) {
        this.futureCB = futureCB;
    }
    public IPromise getFutureCB() {
        return futureCB;
    }

    @Override
    public String toString() {
        return "CallEntry{" +
                   "method=" + method.getName() +
//                   ", args=" + Arrays.toString(args) +
                   ", futureCB=" + futureCB +
                   ", target=" + target +
                   ", sendingActor=" + sendingActor +
                   ", targetActor=" + targetActor +
                   ", onCBQueue=" + onCBQueue +
                   '}';
    }

    public boolean isCallback() {
        return onCBQueue;
    }

    public CallEntry trackingId(int trackingId) {
        this.trackingId = trackingId;
        return this;
    }

    public int getTrackingId() {
        return trackingId;
    }
}
