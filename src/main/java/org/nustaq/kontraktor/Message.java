package org.nustaq.kontraktor;

import java.io.Serializable;
import java.lang.reflect.Method;

/**
 * interface for a message. Currenlty only implemented by CallEntry.
 * Rarely used currently, but can be used for untyped actors / generic redirecting of messages
 * later on.
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

}
