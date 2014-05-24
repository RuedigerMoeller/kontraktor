package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.*;

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

    @Override
    public IFuture send(T target) {
        return withTarget(target,true).send();
    }

    public IFuture yield(T... targets) {
        return new MessageSequence(this, targets).yield();
    }

    public IFuture exec(T... targets) {
        return new MessageSequence(this, targets).exec();
    }

    public Message copy() {
        return withTarget(target, true);
    }

    public Message withTarget(T newTarget) {
        return withTarget(target, false);
    }

    public Message withTarget(T newTarget, boolean copyArgs) {
        DispatcherThread newDispatcher = dispatcher;
        if ( newTarget instanceof ActorProxy )
            newTarget = (T) ((ActorProxy) newTarget).getActor();
        if ( newTarget instanceof Actor) {
            newDispatcher = ((Actor) newTarget).getDispatcher();
        }
        if ( copyArgs ) {
            Object argCopy[] = new Object[args.length];
            System.arraycopy(args, 0, argCopy, 0, args.length);
            return new CallEntry(newTarget, method, argCopy, newDispatcher);
        } else {
            return new CallEntry(newTarget, method, args, newDispatcher);
        }
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
