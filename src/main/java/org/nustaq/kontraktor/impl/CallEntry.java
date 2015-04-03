package org.nustaq.kontraktor.impl;

import org.nustaq.kontraktor.*;

import java.lang.reflect.Method;

/**
* Created by ruedi on 18.05.14.
*/
public class CallEntry<T> implements Message<T> {

    final private Method method;
    final private Object[] args;
    private IPromise futureCB;
    transient final private T target;    // target and target actor are not necessary equal. E.g. target can be callback, but calls are put onto sendingActor Q
    transient private Actor sendingActor; // defines the sender of this message. null in case of outside call
    transient private Actor targetActor;  // defines actor assignment in case target is callback
    transient private boolean onCBQueue;  // determines queue used

    public CallEntry(T target, Method method, Object[] args, Actor sender, Actor targetActor, boolean isCB) {
        this.target = target;
        this.method = method;
        this.args = args;
        this.sendingActor = sender;
        this.targetActor = targetActor;
        this.onCBQueue = isCB;
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

    @Override
    public Actor getSendingActor() {
        return sendingActor;
    }

//    public Message copy() {
//        return withTarget(target, true);
//    }

//    public Message withTarget(T newTarget) {
//        return withTarget(target, false);
//    }

//    public Message withTarget(T newTarget, boolean copyArgs) {
//        Actor targetActor = null;
//        if ( newTarget instanceof ActorProxy )
//            newTarget = (T) ((ActorProxy) newTarget).getActor();
//        if ( newTarget instanceof Actor) {
//            targetActor = (Actor) newTarget;
//        }
//        if ( copyArgs ) {
//            Object argCopy[] = new Object[args.length];
//            System.arraycopy(args, 0, argCopy, 0, args.length);
//            return new CallEntry(newTarget, method, argCopy, targetActor);
//        } else {
//            return new CallEntry(newTarget, method, args, targetActor);
//        }
//    }

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
}
