package de.ruedigermoeller.kontraktor.impl;

import java.lang.reflect.Method;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry {
    final private Object target;
    final private Method method;
    final private Object[] args;
    final boolean isVoid;

    public CallEntry(Object actor, Method method, Object[] args, boolean isVoid) {
        this.target = actor;
        this.method = method;
        this.args = args;
        this.isVoid = isVoid;
    }

    public Object getTarget() {
        return target;
    }
    public Method getMethod() {
        return method;
    }
    public Object[] getArgs() { return args; }

    public boolean isVoid() {
        return isVoid;
    }
    public boolean isAnswered() {
        return result != null || !isVoid;
    }

}
