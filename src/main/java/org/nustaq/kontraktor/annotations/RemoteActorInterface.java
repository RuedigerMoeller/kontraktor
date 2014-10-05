package org.nustaq.kontraktor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ruedi on 28.09.14.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})

/**
 * Created by ruedi on 06.05.14.
 *
 * flags an actor as being implemented elsewhere (e.g. in another language). Methods should be empty.
 * Actually this should be an interface, however the proxying mechanics of kontraktor need a real class
 *
 */
public @interface RemoteActorInterface {
}
