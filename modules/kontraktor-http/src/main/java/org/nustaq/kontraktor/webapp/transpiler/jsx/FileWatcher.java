package org.nustaq.kontraktor.webapp.transpiler.jsx;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.util.Log;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * currently only one app/transpiler instance can be supported via singleton (as setfiles removes previously registered files)
 */
public class FileWatcher extends Actor<FileWatcher> {

    static FileWatcher singleton;
    public static FileWatcher get() {
        synchronized (FileWatcher.class) {
            if ( singleton == null ) {
                singleton = AsActor(FileWatcher.class);
                singleton.startWatching();
            }
            return singleton;
        }
    }

    List<WatchedFile> watched = new ArrayList<>();
    boolean doStop = false;

    public void setFiles(List<WatchedFile> watched) {
        this.watched = watched;
    }

    public void stopWatching() {
        doStop = true;
        stop();
    }

    List<Callback> watchers = new ArrayList<>();
    public IPromise addListener(Callback fileWatcher) {
        watchers.add(fileWatcher);
        return resolve();
    }

    public void startWatching() {
        cyclic(30, () -> {
            watched.forEach( watchedFile -> {
                Long l = watchedFile.getLastModified();
                if ( l != watchedFile.getFile().lastModified() ) {
                    Log.Info(this, "File "+watchedFile.getFile().getAbsolutePath()+" was modified");
                    watchedFile.updateTS();
                    watchedFile.transpiler.updateJSX(watchedFile.getFile(),watchedFile.resolver);
                    fireChange(watchedFile.getWebPath());
                }
            });
            return !doStop;
        });
    }

    private void fireChange(String webPath) {
        watchers = watchers.stream().filter( cb -> ! cb.isTerminated() ).collect(Collectors.toList());
        watchers.forEach( w -> w.pipe(webPath));
    }

}
