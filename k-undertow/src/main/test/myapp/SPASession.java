package myapp;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
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

    ////////////////////////////// test

    public void $testVoid( int a, String b ) {
        System.out.println("session void method called "+a+" "+b);
    }

    public void $testCB( int a, String b, Callback cb) {
        cb.stream("A");
        cb.stream("B");
        cb.stream("C");
        cb.finish();
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
