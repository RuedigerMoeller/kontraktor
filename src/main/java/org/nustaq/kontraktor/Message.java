package org.nustaq.kontraktor;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * Created by ruedi on 23.05.14.
 */
public interface Message<T> extends Serializable {
    /**
     * @return the target of this call. In case of actors this is *NOT* the
     * actor reference/proxy but the real actor object.
     */
    public T getTarget();
    public Method getMethod();

    /**
     * @return a direct reference to the arguments of this messages.
     * if the array is modified, always copy before sending.
     */
    public Object[] getArgs();
    public Actor getSendingActor();

    /**
     * @return the same message, but with copied argument array.
     * I arguments are modified, always use copy before sending, else
     * unpredictable side effects will happen: e.g. msg.copy().send();
     */
//    public Message copy();

    /**
     * @param newTarget
     * @return a shallow copy of this message with a new target set.
     * In case an actorProxy is passed, it is automatically resolved to the underlying actor
     */
//    public Message withTarget(T newTarget);
//    public Message withTarget(T newTarget, boolean copyArgs);
}
