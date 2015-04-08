package myapp;

import com.sun.scenario.effect.impl.sw.sse.SSEBlend_SRC_OUTPeer;
import org.nustaq.kontraktor.Callback;
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

    ////////////////// test

    public void $testVoid( int a, String b ) {
        System.out.println("void method called "+a+" "+b);
    }

    public void $testCB( int a, String b, Callback cb) {
        delayed( 500, () -> cb.stream(a));
        delayed( 1000, () -> cb.stream(b) );
        delayed( 1500, () -> cb.finish() );
    }

    public IPromise $testPromise( String  s ) {
        Promise<Object> objectPromise = new Promise<>();
        delayed(500, () -> objectPromise.resolve(s));
        return objectPromise;
    }

    public IPromise<SPAPojo> $createPojo() {
        return new Promise<>(new SPAPojo().setVals());
    }

    public IPromise<SPAPojo> $promisePojo( SPAPojo poj ) {
        return new Promise<>(poj);
    }

    public IPromise<SPAPojo> $callbackPojo( SPAPojo in, Callback<SPAPojo> poj ) {
        delayed( 500, () -> poj.resolve(in) );
        return new Promise<>(in);
    }

}
