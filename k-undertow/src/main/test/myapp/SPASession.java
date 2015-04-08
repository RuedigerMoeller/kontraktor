package myapp;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.spa.FourKSession;
import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 07/04/15.
 */
@GenRemote
public class SPASession extends FourKSession<SPAServer,SPASession> {

    @Override @Local
    public void $initFromServer(String sessionId, SPAServer spaServer, Object resultFromIsLoginValid) {
        super.$initFromServer(sessionId, spaServer, resultFromIsLoginValid);
        // session init
        Log.Info(this, "session "+resultFromIsLoginValid+" started");
    }

    @Override @Local
    public void $hasBeenUnpublished() {
        // cleanup stuff
        super.$hasBeenUnpublished(); // disconnects
    }

    @Override
    public IPromise $receive(Object message) {
        System.out.println("Session received: '"+message+"'");
        return super.$receive(message);
    }
}
