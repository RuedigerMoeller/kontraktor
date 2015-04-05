package org.nustaq.kontraktor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ruedi on 04.04.2015.
 *
 * Remoted Actor classes can be annotaded with this to either optimize (for binary encoding) or
 * even define the set of known classes (Http/Rest remoting)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Register {
    Class[] value();
}
