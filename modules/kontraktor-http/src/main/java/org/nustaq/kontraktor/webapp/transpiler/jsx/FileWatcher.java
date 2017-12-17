package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileWatcher extends Actor<FileWatcher> {

    List<File> watched = new ArrayList<>();
    Map<File,Long> timestamps = new HashMap<>();
    boolean doStop = false;

    public void setFiles(List<File> watched) {
        this.watched = watched;
    }

    public void stopWatching() {
        doStop = true;
        stop();
    }

    public void startWatching() {
        cyclic(100, () -> {
            List<File> changed = new ArrayList();
//            Log.Info(this,"checking "+watched.size()+" files ..");
            watched.forEach( fi -> {
                Long l = timestamps.get(fi);
                if ( l == null ) {
                    l = fi.lastModified();
                    timestamps.put(fi,l);
                }
                if ( l != fi.lastModified() ) {
                    Log.Info(this, "File "+fi.getAbsolutePath()+" was modified");
                    changed.add(fi);
                    timestamps.put(fi,fi.lastModified());
                }
            });
            return !doStop;
        });
    }

}
