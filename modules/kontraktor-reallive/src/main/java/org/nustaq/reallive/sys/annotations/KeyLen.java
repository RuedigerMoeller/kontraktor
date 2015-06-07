package org.nustaq.reallive.sys.annotations;

/**
 * Created by ruedi on 23.07.14.
 */

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ruedi on 21.07.14.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface KeyLen {
    int value();
}
