package microservice.pub;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

public abstract class IStatelessService<T extends Actor> extends Actor<T> {

    public abstract IPromise<Long> getTime(long delay);

}
