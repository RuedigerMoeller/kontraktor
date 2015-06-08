package sample.httpjs;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.base.RemotedActor;

import java.util.List;

/**
 * Created by ruedi on 30/05/15.
 *
 * per client session state+api obtained if a client successfully logged in
 *
 */
public class KOAppSession extends Actor<KOAppSession> implements RemotedActor {

    KOHttpApp app;

    public void init(KOHttpApp app, List<String> todo) {
        this.app = app;
    }

    @Override
    public void hasBeenUnpublished() {
        app.clientClosed(self());
        self().stop();
    }
}
