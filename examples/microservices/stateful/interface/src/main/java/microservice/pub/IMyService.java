package microservice.pub;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

public abstract class IMyService<T extends Actor> extends Actor<T> {

    public abstract IPromise publishItem(Item item);
    public abstract IPromise<String> addChangeListener(Callback<ChangeMessage> listener);
    public abstract void removeChangeListener(String id);

}
