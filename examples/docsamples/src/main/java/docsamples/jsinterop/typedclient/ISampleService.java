package docsamples.jsinterop.typedclient;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

/**
 * dummy actor to provide typed methods for SampleService.js
 */
public class ISampleService extends Actor<ISampleService> {

    public void withCallback(String astring, Callback callback) {
        throw new NotImplementedException();
    }

    public IPromise<String> withCallbackAndPromise(String astring, Callback callback) {
        throw new NotImplementedException();
    }

    public void voidFun() {
        throw new NotImplementedException();
    }

    public IPromise withPromise(String aString) {
        throw new NotImplementedException();
    }

    public void automaticPromised(String aString) {
        throw new NotImplementedException();
    }

    public IPromise<ISampleSubService> getSingletonSubserviceTyped(String credentials) {
        throw new NotImplementedException();
    }
}
