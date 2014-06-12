package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

import java.lang.reflect.Method;
import java.util.Queue;

/**
 * Created by ruedi on 23.05.14.
 */
public interface Message<T> {
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
    public Actor getTargetActor();
    public Future send();
    public Future send(T target);
    public Future yield(T ... targets);
    public Future exec(T ... targets);

    /**
     * @return the same message, but with copied argument array.
     * I arguments are modified, always use copy before sending, else
     * unpredictable side effects will happen: e.g. msg.copy().send();
     */
    public Message copy();

    /**
     * @param newTarget
     * @return a shallow copy of this message with a new target set.
     * In case an actorProxy is passed, it is automatically resolved to the underlying actor
     */
    public Message withTarget(T newTarget);
    public Message withTarget(T newTarget, boolean copyArgs);
}
