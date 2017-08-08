package docsamples.jsinterop.jsserves.typedclient;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Dummy to provide typed signatures
 */
public class IGreeter extends Actor<IGreeter> {

    public IPromise<String> greet(String name, long duration ) {
        throw new NotImplementedException();
    }

}
