package myapp;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.kontraktor.remoting.spa.FourK;

/**
 * Created by ruedi on 07/04/15.
 */
@GenRemote
public class SPAServer extends FourK<SPAServer,SPASession> {

    @Override
    protected IPromise<Object> isLoginValid(String user, String pwd) {
        if ("me".equals(user)) {
            return new Promise<>("myUserContext");
        }
        return new Promise<>(null);
    }

    @Override
    protected SPASession createSessionActor(String sessionId, Scheduler clientScheduler, Object resultFromIsLoginValid) {
        SPASession spaSession = AsActor(SPASession.class);
        return spaSession;
    }

    @Override
    public IPromise $receive(Object message) {
        System.out.println("Service received: '"+message+"'");
        return super.$receive(message);
    }

}
