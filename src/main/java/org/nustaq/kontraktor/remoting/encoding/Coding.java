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

package org.nustaq.kontraktor.remoting.encoding;

import org.nustaq.serialization.FSTConfiguration;

import java.util.Arrays;
import java.util.function.Consumer;

/**
 * Created by ruedi on 26.10.14.
 *
 * umh .. unfinished concept of custom serializers
 *
 */
public class Coding {
    SerializerType coding;
    Class crossPlatformShortClazzNames[];

    public Coding(SerializerType coding) {
        this.coding = coding;
    }

    public Coding(SerializerType coding, Class ... crossPlatformShortClazzNames) {
        this.coding = coding;
        this.crossPlatformShortClazzNames = crossPlatformShortClazzNames;
    }

    public Class[] getCrossPlatformShortClazzNames() {
        return crossPlatformShortClazzNames;
    }

    public SerializerType getCoding() {
        return coding;
    }

    public FSTConfiguration createConf() {
        FSTConfiguration conf;
        switch (coding) {
            case MinBin:
                conf = FSTConfiguration.createMinBinConfiguration();
                break;
            case Json:
                conf = FSTConfiguration.createJsonConfiguration();
                break;
            case JsonNoRef:
                conf = FSTConfiguration.createJsonConfiguration(false, false);
                break;
            case JsonNoRefPretty:
                conf = FSTConfiguration.createJsonConfiguration(true, false);
                break;
            case UnsafeBinary:
                conf = FSTConfiguration.createFastBinaryConfiguration();
                break;
            case FSTSer:
                conf = FSTConfiguration.createDefaultConfiguration();
                break;
            default:
                throw new RuntimeException("unknown ser configuration type");
        }
        return conf;
    }

    @Override
    public String toString() {
        return "Coding{" +
                   "coding=" + coding +
                   '}';
    }
}
