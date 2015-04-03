package kontraktor.remoting.triangle;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

/**
 * Created by ruedi on 13.08.2014.
 */
public class OuterActor extends Actor<OuterActor> {

    private int myid;

    public void $init( int id ) {
        myid = id;
    }

    // lookup a remote ref in center and do a call on the result outeractor. The result
    // of calling remote outer is then fed into the future returned by this method
    public IPromise<String> $sendCall( CenterActor center, int id, String msg ) {
        Promise<String> result = new Promise();
        center.$getOuter(id).then( (outer,err) -> {
            if (outer == null ) {
                result.complete("id " + id + " not found.", null);
            } else {
                outer.$testCall(msg).then( (s,err1) -> result.complete(s, err) );
            }
        });
        return result;
    }

    public IPromise<String> $testCall(String s) {
        return new Promise<>(myid+" responds: "+s);
    }
}
