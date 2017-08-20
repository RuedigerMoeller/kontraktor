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

package org.nustaq.kontraktor.webapp.javascript;

import java.io.*;

/**
 * Created by ruedi on 06.04.2015.
 *
 * Algorithm: starting from the root component (searched via resource path), resolve all dependencies
 * resulting in a list of (ordered according to dependencies) directories.
 * The resulting list is then searched in order when doing lookup/merge
 */
public class DependencyResolver implements HtmlImportShim.ResourceLocator{

    private final HtmlImportShim.ResourceLocator resourceLocator;
    protected File resourcePath[];
    protected String baseDir = ".";

    public DependencyResolver(String baseDir, String[] resourcePath, HtmlImportShim.ResourceLocator rl) {
        this.baseDir = baseDir;
        setResourcePath(resourcePath);
        this.resourceLocator = rl;
    }

    public String getBaseDir() {
        return baseDir;
    }

    // must be called before setting path and base comp
    protected DependencyResolver setBaseDir(String baseDir) {
        this.baseDir = baseDir;
        return this;
    }

    protected DependencyResolver setResourcePath( String ... path ) {
        if ( path == null ) {
            resourcePath = new File[0];
            return this;
        }
        resourcePath = new File[path.length];
        for (int i = 0; i < path.length; i++) {
            String dir;
            if ( path[i].startsWith("/") ) {
                dir = path[i];
            } else {
                dir = baseDir+"/"+path[i];
            }
            File f = new File( dir ).getAbsoluteFile();
            if ( f.exists() && ! f.isDirectory() ) {
                throw new RuntimeException("only directorys can reside on resourcepath");
            }
            resourcePath[i] = f;
        }
        return this;
    }

    /**
     * iterate component directories in order and return full path of first file matching
     *
     * @param name
     * @return
     */
    @Override
    public File locateResource( String name ) {
        for (int i = 0; i < resourcePath.length; i++) {
            File fi = new File(resourcePath[i].getAbsolutePath()+File.separator+name);
            if ( fi.exists() )
                return fi;
        }
        return null;
    }

    /**
     * this part of locator interface is only implemented if another
     * resourcelocator is set.
     *
     * @param impFi
     * @return
     */
    @Override
    public byte[] retrieveBytes(File impFi) {
        if ( resourceLocator != null ) {
            // does only return a result if a transpiler is registered
            byte[] bytes = resourceLocator.retrieveBytes(impFi);
            if ( bytes != null )
                return bytes;
        }
        return HtmlImportShim.ResourceLocator.super.retrieveBytes(impFi);
    }

    public String resolveUniquePath(File fi) {
        String longestMatch = "";
        try {
            String fp = fi.getCanonicalPath();
            for (int i = 0; i < resourcePath.length; i++) {
                String p = resourcePath[i].getCanonicalPath();
                if ( fp.startsWith(p) && p.length() > longestMatch.length() )
                    longestMatch = p;
            }
            return fp.substring(longestMatch.length());
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

}
