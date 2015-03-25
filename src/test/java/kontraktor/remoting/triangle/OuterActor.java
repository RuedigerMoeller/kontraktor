package kontraktor.remoting.triangle;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

/**
 * Created by ruedi on 13.08.2014.
 */
public class OuterActor extends Actor<OuterActor> {

    private CenterActor center;
    private int myid;

    public void $init( int id, CenterActor center ) {
        this.center = center;
        myid = id;
        center.$registerRemoteRef( id, self() );
    }

    // lookup a remote ref in center and do a call on the result outeractor. The result
    // of calling remote outer is then fed into the future returned by this method
    public Future<String> $sendCall( int id, String msg ) {
        Promise<String> result = new Promise();
        center.$getOuter(id).then( (outer,err) -> {
            if (outer == null ) {
                result.settle("id " + id + " not found.", null);
            } else {
                outer.$testCall("hallo from "+myid).then( (s,err1) -> result.settle(s, err) );
            }
        });
        return result;
    }

    public Future<String> $testCall(String s) {
        return new Promise<>(myid+" responds: "+s);
    }
}
