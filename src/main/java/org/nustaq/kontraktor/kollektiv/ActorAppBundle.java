package org.nustaq.kontraktor.kollektiv;

import org.nustaq.kontraktor.Actor;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

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

    String name;
    HashMap<String,CPEntry> resources = new HashMap<>();
    Actor mainActor;

    transient String baseDir;
    transient MemberClassLoader loader;

    public void put(String normalizedPath, byte[] bytes) {
        resources.put(normalizedPath, new CPEntry(bytes, normalizedPath));
    }

    public ActorAppBundle(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setResources(HashMap<String, CPEntry> resources) {
        this.resources = resources;
    }

    public Actor getMainActor() {
        return mainActor;
    }

    public void setMainActor(Actor mainActor) {
        this.mainActor = mainActor;
    }

    public HashMap<String, CPEntry> getResources() {
        return resources;
    }

    public int getSizeKB() {
        int sum = 0;
        for (Iterator<Map.Entry<String, CPEntry>> iterator = resources.entrySet().iterator(); iterator.hasNext(); ) {
            Map.Entry<String, CPEntry> next = iterator.next();
            sum += next.getValue().bytes.length;
        }
        return sum/1000;
    }

    public String getBaseDir() {
        return baseDir;
    }

    public void setBaseDir(String baseDir) {
        this.baseDir = baseDir;
    }

    public MemberClassLoader getLoader() {
        return loader;
    }

    public void setLoader(MemberClassLoader loader) {
        this.loader = loader;
    }

    public byte[] findClass(String name) {
        name = name.replace( '.', File.separatorChar );
        Path path = Paths.get(baseDir + File.separator + name + ".class");
        if ( path.toFile().exists() ) {
            try {
                return Files.readAllBytes(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }


}
