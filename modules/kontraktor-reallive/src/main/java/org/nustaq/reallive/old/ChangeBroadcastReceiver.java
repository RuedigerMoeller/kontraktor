package org.nustaq.reallive.old;

/**
 * Created by ruedi on 05.07.14.
 */
public interface ChangeBroadcastReceiver<T extends Record> {

    void onChangeReceived(ChangeBroadcast<T> changeBC);

}
