package org.nustaq.kontraktor.rest.doc;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ApiOp {
    String summary() default "";
    String description() default "";
    Class response() default Void.class;

    /**
     * usually taken from the method signature, however sometimes documentation should deviate form type given.
     *
     * @return
     */
    Class docPostDataDTO() default Void.class;
    String container() default "";
    String requestContainer() default "";
}
