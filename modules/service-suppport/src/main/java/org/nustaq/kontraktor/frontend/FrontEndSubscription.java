package org.nustaq.kontraktor.frontend;

import org.nustaq.kontraktor.Callback;
import org.nustaq.reallive.api.Subscriber;

/**
 * Created by ruedi on 30/08/15.
 */
public class FrontEndSubscription {

    Subscriber subscriber;
    String tableName;
    Callback frontEndCallback;

    public FrontEndSubscription(Subscriber subscriber, String tableName, Callback frontEndCallback) {
        this.subscriber = subscriber;
        this.tableName = tableName;
        this.frontEndCallback = frontEndCallback;
    }

    public Subscriber getSubscriber() {
        return subscriber;
    }

    public String getTableName() {
        return tableName;
    }

    public Callback getFrontEndCallback() {
        return frontEndCallback;
    }
}
