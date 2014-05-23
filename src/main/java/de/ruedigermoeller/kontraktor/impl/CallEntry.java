package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.IFuture;
import de.ruedigermoeller.kontraktor.Message;

import java.lang.reflect.Method;
import java.util.Arrays;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry<T> implements Message<T> {
    final private T target;
    final private Method method;
    final private Object[] args;
    private IFuture futureCB;
    private DispatcherThread dispatcher;

    public CallEntry(T actor, Method method, Object[] args, DispatcherThread disp) {
        this.target = actor;
        this.method = method;
        this.args = args;
        this.dispatcher = disp;
    }

    public T getTarget() {
        return target;
    }
    public Method getMethod() {
        return method;
    }
    public Object[] getArgs() { return args; }

    @Override
    public DispatcherThread getDispatcher() {
        return dispatcher;
    }

    @Override
    public IFuture send() {
        return DispatcherThread.pollDispatchOnObject(DispatcherThread.getThreadDispatcher(), this);
    }

    public boolean hasFutureResult() {
        return method.getReturnType() == IFuture.class;
    }

    public void setFutureCB(IFuture futureCB) {
        this.futureCB = futureCB;
    }
    public IFuture getFutureCB() {
        return futureCB;
    }

    @Override
    public String toString() {
        return "CallEntry{" +
                "target=" + target +
                ", method=" + method +
                ", args=" + Arrays.toString(args) +
                ", futureCB=" + futureCB +
                ", dispatcher=" + dispatcher +
                '}';
    }
}
