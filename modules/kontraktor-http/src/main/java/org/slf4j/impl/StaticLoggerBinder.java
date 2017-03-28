/*
Kontraktor-Http Copyright (c) Ruediger Moeller, All rights reserved.

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

package org.slf4j.impl;

import org.nustaq.kontraktor.loggingadapter.KSL4jLoggerFactory;
import org.slf4j.ILoggerFactory;

/**
 * Created by ruedi on 11/06/15.
 */
public class StaticLoggerBinder {

    public static KSL4jLoggerFactory fac = new KSL4jLoggerFactory();
    public static StaticLoggerBinder instance = new StaticLoggerBinder();

    public static StaticLoggerBinder getSingleton() {
        return instance;
    }

    public ILoggerFactory getLoggerFactory() {
        return fac;
    }

    public String getLoggerFactoryClassStr() {
        return "";
    }
}
