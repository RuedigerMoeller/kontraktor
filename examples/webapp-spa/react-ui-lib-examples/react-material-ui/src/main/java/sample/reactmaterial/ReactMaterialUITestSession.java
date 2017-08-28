package sample.reactmaterial;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.RemotedActor;

public class ReactMaterialUITestSession extends Actor<ReactMaterialUITestSession> implements RemotedActor {

    private String name;
    private ReactMaterialUITestApp app;

    public void init(String name, ReactMaterialUITestApp app) {
        this.app = app;
        this.name = name;
    }

    public IPromise<String> greet(String who) {
        return new Promise("Hello "+who+" from "+name);
    }

    /**
     * interface RemotedActor, session time out notification callback
     * @param connectionIdentifier
     */
    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {
    }

    @Override
    public void hasBeenPublished(String connectionIdentifier) {
        // associate user identity with sessionid for resurrection
        app.registerSessionData(connectionIdentifier,name);
    }
}
