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

package org.nustaq.kontraktor.remoting.http.builder;

/**
 * Created by ruedi on 09.06.2015.
 */
public class CFGDirRoot {

    String urlPath;
    String dir;

    public CFGDirRoot(String urlPath, String dir) {
        this.dir = dir;
        this.urlPath = urlPath;
    }

    public String getUrlPath() {
        return this.urlPath;
    }

    public String getDir() {
        return this.dir;
    }

    public CFGDirRoot urlPath(final String urlPath) {
        this.urlPath = urlPath;
        return this;
    }

    public CFGDirRoot dir(final String dir) {
        this.dir = dir;
        return this;
    }


}
