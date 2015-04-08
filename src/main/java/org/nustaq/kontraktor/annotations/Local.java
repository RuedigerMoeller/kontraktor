package org.nustaq.kontraktor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD,ElementType.TYPE})

/**
 *
 * method modifier to signal this method should not be exposed via an remoting interface (process local message)
 *
 * Created by ruedi on 18.10.14.
 */
public @interface Local {
}
