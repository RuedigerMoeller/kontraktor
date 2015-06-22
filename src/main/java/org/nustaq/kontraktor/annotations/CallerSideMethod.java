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

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})

/**
 * Created by ruedi on 06.05.14.
 *
 * Specifies this method is a utility processed on client side / inside sender thread.
 * e.g.
 *
 * class .. extends Actor {
 *     public void message( long timeStamp, String stuff ) {..}
 *
 *     // just an utility executed inside calling thread
 *     @CallerSideMethod public void message( String stuff ) {
 *         message( System.currentTimeMillis(), stuff );
 *     }
 * }
 *
 * Note those method cannot access local state of the actor, they just might invoke methods.
 */
public @interface CallerSideMethod {
}
