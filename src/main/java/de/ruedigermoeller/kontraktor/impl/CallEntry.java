package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.Callback;

import java.lang.reflect.Method;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry {
    final private Object target;
    final private Method method;
    final private Object[] args;
    private Callback futureCB;

    public CallEntry(Object actor, Method method, Object[] args) {
        this.target = actor;
        this.method = method;
        this.args = args;
    }

    public Object getTarget() {
        return target;
    }
    public Method getMethod() {
        return method;
    }
    public Object[] getArgs() { return args; }

    public void setFutureCB(Callback futureCB) {
        this.futureCB = futureCB;
    }

    public Callback getFutureCB() {
        return futureCB;
    }
}
