/*
Kontraktor Copyright (c) Ruediger Moeller, All rights reserved.

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 3.0 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

See https://www.gnu.org/licenses/lgpl.txt
 */

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
 * if the sender and receiver happen to be scheduled on the same thread. (callback invocation order is undefined).
 *
 * Additionally callback messages have higher priority compared to actor messages. A dispatcher thread
 * will first check the callback queue before looking for messages on the actors mailbox.
 *
 * Besides performance improvements, this also enables some scheduling tweaks to automatically prevent deadlocks.
 */
public @interface AsCallback {
}
