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

import java.lang.annotation.*;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})

/**
 * Created by moelrue on 07.05.2014.
 *
 * interface typed parameters tagged as InThread get wrapped by a re-enqueuing wrapper automatically. Calls on these object are
 * executed in the callers thread then (enqueued to the calling Actors queue).
 * (Automatically applied for built in Callback + Promise class)
 *
 */
public @interface InThread {
}
