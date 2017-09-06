package microservice.impl;

import microservice.pub.ChangeMessage;
import microservice.pub.IMyService;
import microservice.pub.Item;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.Local;

import java.util.*;

public class MyService extends Actor<MyService> implements IMyService {

    Map<String,Callback<ChangeMessage>> listeners;
    List<Item> items;

    @Local void init() {
        listeners = new HashMap<>();
        items = new ArrayList<>();
    }

    @Override
    public IPromise addItem(Item item) {
        items.add(item);
        fireChange(new ChangeMessage().action(ChangeMessage.Action.ADDED).item(item));
        return resolve(true);
    }

    private void fireChange(ChangeMessage item) {
        listeners.values().forEach( li -> li.pipe(item) );
    }

    @Override
    public IPromise removeItems(String name) {
        return null;
    }

    @Override
    public void findItem(String substring, Callback<Item> result) {
        result.finish();
    }

    @Override
    public IPromise<String> addChangeListener(Callback<ChangeMessage> listener) {
        String id = UUID.randomUUID().toString();
        listeners.put(id,listener);
        return resolve(id);
    }

    @Override
    public void removeChangeListener(String id) {
        listeners.remove(id);
    }

}
