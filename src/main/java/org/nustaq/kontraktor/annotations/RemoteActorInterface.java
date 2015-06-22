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
