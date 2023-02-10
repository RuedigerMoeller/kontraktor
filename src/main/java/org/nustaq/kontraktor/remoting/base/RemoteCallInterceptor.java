package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.annotations.RateLimited;
import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.impl.ActorProxyFactory;
import org.nustaq.kontraktor.util.Log;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;

public class RemoteCallInterceptor implements BiFunction<Actor,String,Boolean> {
    private boolean secured;

    protected Map<String,RateLimitEntry> rateLimits;

    @Override
    public Boolean apply(Actor actor, String methodName) {
        Method method = actor.__getCachedMethod(methodName, actor, null);
        if ( method == null ) {
            Log.Warn(null, "no such method on "+actor.getClass().getSimpleName()+"#"+methodName);
        }
        if ( method == null || ActorProxyFactory.getInheritedAnnotation(Local.class,method) != null || Modifier.isStatic(method.getModifiers())) {
            return false;
        }

        // fixme: cache this
        RateLimited rateLimited = ActorProxyFactory.getInheritedAnnotation(RateLimited.class, method);
        if ( rateLimited != null ) {
//            synchronized (this)
            {
                if (rateLimits == null) {
                    rateLimits = new ConcurrentHashMap();
                    rateLimits.put(method.getName(), new RateLimitEntry(rateLimited));
                }
            }
        }
        // fixme: this slows down remote call performance somewhat.
        // checks should be done before putting methods into cache
        if ( secured && ActorProxyFactory.getInheritedAnnotation(Remoted.class,method) == null ) {
            Log.Warn(null, "method not @Remoted "+actor.getClass().getSimpleName()+"#"+methodName);
            return false;
        }
        return true;
    }
}
