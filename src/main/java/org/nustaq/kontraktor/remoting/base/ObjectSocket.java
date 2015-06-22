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

package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

import java.io.IOException;

/**
 * Created by moelrue on 5/7/15.
 *
 * capability to send objects
 *
 */
public interface ObjectSocket {

    public void writeObject(Object toWrite) throws Exception;

    public void flush() throws Exception;

    public void setLastError(Throwable ex);

    public Throwable getLastError();

    /**
     * set by outer machinery
     * @param conf
     */
    public void setConf( FSTConfiguration conf );

    public FSTConfiguration getConf();

    void close() throws IOException;

    default boolean canWrite() {
        return true;
    }
}
