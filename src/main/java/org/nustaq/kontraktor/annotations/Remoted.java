package org.nustaq.kontraktor.annotations;

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@Inherited

/**
 * @see @Secured
 */
public @interface Remoted {
}
