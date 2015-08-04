package org.nustaq.reallive.newimpl;

import java.util.function.*;

/**
 * Created by moelrue on 04.08.2015.
 */
public class FilteringListener<K,V extends Record<K>> implements ChangeReceiver<K,V> {

    @Override
    public void receive(ChangeMessage<K, V> change) {
        switch (change.getType()) {
            case ChangeMessage.ADD:
                break;
            case ChangeMessage.UPDATE:
                break;
            case ChangeMessage.REMOVE:
                break;
        }
    }
}
