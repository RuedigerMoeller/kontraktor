package microservice.pub;

import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

public interface IMyService {

    IPromise addItem(Item item);
    IPromise removeItems(String name);
    void findItem(String substring, Callback<Item> result);
    IPromise<String> addChangeListener(Callback<ChangeMessage> listener);
    void removeChangeListener(String id);
}
