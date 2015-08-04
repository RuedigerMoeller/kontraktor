package org.nustaq.reallive.newimpl;

/**
 * Created by ruedi on 03/08/15.
 *
 * propagates change events to listener according to their filter
 *
 */
public class ChangeDispatcher<K,V extends Record<K>> implements ChangeReceiver<K,V> {
    @Override
    public void receive(ChangeMessage<K, V> change) {

    }
}
