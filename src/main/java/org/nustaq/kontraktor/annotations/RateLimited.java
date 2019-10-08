package org.nustaq.kontraktor.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited

/**
 * once a method exceeds its limit it won't be called. In case a promise is returned, an error is sent back.
 * this annotation is only evaluated in case of remote calls.
 * Warning: if call is delayed by rate limit order of execution might mess up
 */
public @interface RateLimited {
    int callsPerSecond() default 1000;
    int callsPerMinute() default 0;
}
