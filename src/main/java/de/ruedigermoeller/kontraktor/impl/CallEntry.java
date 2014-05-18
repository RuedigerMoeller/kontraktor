package de.ruedigermoeller.kontraktor.impl;

import java.lang.reflect.Method;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry {
    public static final String NULL_RESULT = "__N_U_L_L__";
    final private Object target;
    final private Method method;
    final private Object[] args;
    final boolean isYield;
    private volatile Object result; // FIXME: volatile ?

    public CallEntry(Object actor, Method method, Object[] args, boolean isYield) {
        this.target = actor;
        this.method = method;
        this.args = args;
        this.isYield = isYield;
    }

    public Object getTarget() {
        return target;
    }
    public Method getMethod() {
        return method;
    }
    public Object[] getArgs() { return args; }

    public Object getResult() {
        if ( result == NULL_RESULT )
            return null;
        return result;
    }

    public void setResult(Object result) {
        this.result = result;
    }

    public boolean isYield() {
        return isYield;
    }

    public boolean isAnswered() {
        return result != null || !isYield;
    }
}
