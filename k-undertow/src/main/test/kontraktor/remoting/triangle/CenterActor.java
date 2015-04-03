package kontraktor.remoting.triangle;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

import java.util.HashMap;

/**
 * Created by ruedi on 13.08.2014.
 */
public class CenterActor extends Actor<CenterActor> {

    HashMap<Integer,OuterActor> registry = new HashMap<>();

    public void $registerRemoteRef( int id, OuterActor outer ) {
        registry.put(id,outer);
    }

    public IPromise<OuterActor> $getOuter(int id) {
        return new Promise<>(registry.get(id));
    }


}
