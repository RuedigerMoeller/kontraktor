package org.nustaq.reallive.newimpl;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeReceiver<T extends Record> {

    public void receive(ChangeMessage change);

}
