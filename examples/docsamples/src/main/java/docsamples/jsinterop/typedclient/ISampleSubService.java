package docsamples.jsinterop.typedclient;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * Dummy Interfacing class mirroring SampleService.js#SubService
 */
public class ISampleSubService extends Actor<ISampleSubService> {

    public IPromise withCallbackAndPromise( String astring, Callback callback) {
        throw new NotImplementedException();
    }

    public void voidFun() {
        throw new NotImplementedException();
    }

}
