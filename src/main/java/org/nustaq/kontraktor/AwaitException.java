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

package org.nustaq.kontraktor;

/**
 * if await is used on a Promise to wait for its result (non-blocking!) any error object
 * returned by the Promise will trigger a this Exception. In case the error
 * object is instance of Throwable, this will be thrown directly.
 */
public class AwaitException extends RuntimeException {
    Object error;

    public AwaitException(Object error) {
        super(""+error);
        this.error = error;
    }

    public Object getError() {
        return error;
    }
}
