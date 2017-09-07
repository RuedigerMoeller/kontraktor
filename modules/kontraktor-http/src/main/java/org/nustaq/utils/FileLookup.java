package org.nustaq.utils;

import java.io.File;

public class FileLookup {

    String fileName2Search;
    String subdirs[] = { ".", "etc", "run/etc" };

    public FileLookup(String fileName2Search) {
        this.fileName2Search = fileName2Search;
    }

    public FileLookup fileName2Search(String fileName2Search) {
        this.fileName2Search = fileName2Search;
        return this;
    }

    public FileLookup subdirs(String[] subdirs) {
        this.subdirs = subdirs;
        return this;
    }

    public File lookup(File curDir) {
        for (int i = 0; i < subdirs.length; i++) {
            String subdir = subdirs[i];
            File sd = new File(curDir,subdir+"/"+fileName2Search);
//            System.out.println("checking "+sd.getAbsolutePath());
            if ( sd.exists() )  {
                return sd;
            }
        }
        File parentFile = curDir.getAbsoluteFile().getParentFile();
        if (parentFile!=null)
            return lookup(parentFile);
        return null;
    }

    public File lookup() {
        return lookup(new File("."));
    }
}
