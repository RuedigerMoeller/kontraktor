package de.ruedigermoeller.kontraktor;

import de.ruedigermoeller.kontraktor.impl.DispatcherThread;

import java.lang.reflect.Method;

/**
 * Created by ruedi on 23.05.14.
 */
public interface Message<T> {
    public T getTarget();
    public Method getMethod();
    public Object[] getArgs();
    public DispatcherThread getDispatcher();
    public IFuture send();
}
