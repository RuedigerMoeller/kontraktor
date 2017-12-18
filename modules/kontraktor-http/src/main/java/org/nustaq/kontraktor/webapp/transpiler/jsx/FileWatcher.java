package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FileWatcher extends Actor<FileWatcher> {

    List<WatchedFile> watched = new ArrayList<>();
    boolean doStop = false;

    public void setFiles(List<WatchedFile> watched) {
        this.watched = watched;
    }

    public void stopWatching() {
        doStop = true;
        stop();
    }

    public void startWatching() {
        cyclic(100, () -> {
            watched.forEach( watchedFile -> {
                Long l = watchedFile.getLastModified();
                if ( l != watchedFile.getFile().lastModified() ) {
                    Log.Info(this, "File "+watchedFile.getFile().getAbsolutePath()+" was modified");
                    watchedFile.updateTS();
                    watchedFile.transpiler.updateJSX(watchedFile.getFile(),watchedFile.resolver);
                }
            });
            return !doStop;
        });
    }

}
