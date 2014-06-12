package de.ruedigermoeller.kontraktor.impl;

import de.ruedigermoeller.kontraktor.*;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Queue;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry<T> implements Message<T> {
    final private T target;    // target and target actor are not necessary equal. E.g. target can be callback, but calls are put onto targetActor Q
    final private Method method;
    final private Object[] args;
    private Future futureCB;
    private Actor targetActor; // defines mailboxes but not necessary target

    public CallEntry(T target, Method method, Object[] args, Actor disp) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.targetActor = disp;
    }

    public T getTarget() {
        return target;
    }
    public Method getMethod() {
        return method;
    }
    public Object[] getArgs() { return args; }

    @Override
    public Actor getTargetActor() {
        return targetActor;
    }

    @Override
    public Future send() {
        return DispatcherThread.Put2QueuePolling(this);
    }

    @Override
    public Future send(T target) {
        return withTarget(target,true).send();
    }

    public Future yield(T... targets) {
        return new MessageSequence(this, targets).yield();
    }

    public Future exec(T... targets) {
        return new MessageSequence(this, targets).exec();
    }

    public Message copy() {
        return withTarget(target, true);
    }

    public Message withTarget(T newTarget) {
        return withTarget(target, false);
    }

    public Message withTarget(T newTarget, boolean copyArgs) {
        Actor targetActor = null;
        if ( newTarget instanceof ActorProxy )
            newTarget = (T) ((ActorProxy) newTarget).getActor();
        if ( newTarget instanceof Actor) {
            targetActor = (Actor) newTarget;
        }
        if ( copyArgs ) {
            Object argCopy[] = new Object[args.length];
            System.arraycopy(args, 0, argCopy, 0, args.length);
            return new CallEntry(newTarget, method, argCopy, targetActor);
        } else {
            return new CallEntry(newTarget, method, args, targetActor);
        }
    }

    public boolean hasFutureResult() {
        return method.getReturnType() == Future.class;
    }

    public void setFutureCB(Future futureCB) {
        this.futureCB = futureCB;
    }
    public Future getFutureCB() {
        return futureCB;
    }

    @Override
    public String toString() {
        return "CallEntry{" +
                "target=" + target +
                ", method=" + method +
                ", args=" + Arrays.toString(args) +
                ", futureCB=" + futureCB +
                ", disp=" + targetActor+
                '}';
    }
}
