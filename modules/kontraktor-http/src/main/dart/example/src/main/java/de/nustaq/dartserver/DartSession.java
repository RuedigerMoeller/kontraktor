package de.nustaq.dartserver;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import java.util.*;

public class DartSession extends Actor<DartSession> implements RemotedActor {

    private DartServer app;
    private Callback uiSubscription; // pipe stuff to ui client
    private Map user;

    @Local
    public void init(Map user, DartServer app, Callback uiSubscription ) {
        this.app = app;
        this.uiSubscription = uiSubscription;
        this.user = user;
        delayed( 10000, () -> uiSubscription.pipe("Hello from server session"));
    }

    public IPromise<String> hello(String other) {
        return resolve("hello "+other);
    }

    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {

    }

    @Override
    public void hasBeenPublished(String connectionIdentifier) {

    }
}
