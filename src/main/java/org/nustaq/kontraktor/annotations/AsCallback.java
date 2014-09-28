package org.nustaq.kontraktor.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Created by ruedi on 28.09.14.
 */

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})

/**
 * Created by ruedi on 06.05.14.
 *
 * handle this method like a callback method. The contract for callbacks is weaker than for regular
 * actor methods. For a callback method, the scheduler is allowed to execute the method synchronous
 * if the sender and receiver happen to be scheduled on the same thread.
 *
 * Additionally callback messages have higher priority compared to regualt actor messages. A dispatcher thread
 * will always first check the callback queue befor looking for messages on the actors mailbox.
 *
 * Besides performance improvements, this also enables some scheduling tweaks to automatically prevent deadlocks.
 */
public @interface AsCallback {
}
