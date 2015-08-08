package org.nustaq.reallive.interfaces;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeReceiver<K,T extends Record> {

    public void receive(ChangeMessage<K,T> change);

}
