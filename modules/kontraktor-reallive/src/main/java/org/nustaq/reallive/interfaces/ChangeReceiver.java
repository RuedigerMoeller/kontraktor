package org.nustaq.reallive.interfaces;

import java.io.Serializable;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeReceiver<K> extends Serializable {

    public void receive(ChangeMessage<K> change);

}
