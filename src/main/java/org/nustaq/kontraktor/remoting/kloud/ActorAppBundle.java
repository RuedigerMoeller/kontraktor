package org.nustaq.kontraktor.remoting.kloud;

import java.io.File;
import java.io.Serializable;
import java.util.List;

/**
 * Created by moelrue on 3/6/15.
 */
public class ActorAppBundle implements Serializable {

    public static class CPEntry implements Serializable {
        byte bytes[];
        String name;

        public CPEntry(byte[] bytes, String name) {
            this.bytes = bytes;
            this.name = name;
        }
    }

    List<CPEntry> classPath;
    String name;

    public ActorAppBundle() {
    }

    public ActorAppBundle(String name, List<CPEntry> classPath ) {
        this.classPath = classPath;
        this.name = name;
    }

    public List<CPEntry> getClassPath() {
        return classPath;
    }

    public void setClassPath(List<CPEntry> classPath) {
        this.classPath = classPath;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

}
