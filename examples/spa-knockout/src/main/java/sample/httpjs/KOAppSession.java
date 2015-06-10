package sample.httpjs;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
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
    String user;

    public void init(KOHttpApp app, List<String> todo, String user) {
        this.app = app;
        this.user = user;
    }

    public void sendMessage( String text ) {
        app.broadCastChatMsg(user,text); // note this is async actor msg, therefore threadsafe
    }

    public void subscribeChat( Callback<KOPushEvent> ev ) {
        app.subscribeChat(self(),ev);
    }

    @Override
    public void hasBeenUnpublished() {
        app.clientClosed(self());
        self().stop();
    }
}
