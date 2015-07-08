/*
Kontraktor-reactivestreams Copyright (c) Ruediger Moeller, All rights reserved.

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
package org.nustaq.kontraktor.reactivestreams;

import org.reactivestreams.Subscription;

/**
 * Created by ruedi on 04/07/15.
 *
 * Can be used to signal a cancel subscription from a kontraktor callback
 *
 */
public class CancelException extends RuntimeException {

    public static final CancelException Instance = new CancelException(null);

    Subscription subs;

    public CancelException(Subscription subs) {
        this.subs = subs;
    }

    public Subscription getSubs() {
        return subs;
    }

    @Override
    public synchronized Throwable fillInStackTrace() {
        return this;
    }

}
