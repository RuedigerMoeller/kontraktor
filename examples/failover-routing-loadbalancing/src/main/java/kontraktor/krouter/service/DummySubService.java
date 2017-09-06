package kontraktor.krouter.service;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.RemotedActor;

public class DummySubService extends Actor<DummySubService> implements RemotedActor {

    private String state;

    public IPromise<String> subMe(String in, Callback cb ) {
        cb.pipe(in);
        cb.pipe(in+" "+in);
        cb.finish();
        return resolve("Yes");
    }

    public void setState(String state) {
        this.state = state;
    }

    public IPromise getState() {
        return resolve(state);
    }

    @Override
    public void hasBeenUnpublished(String connectionIdentifier) {
        System.out.println("sub actor unpublished");
    }
}
