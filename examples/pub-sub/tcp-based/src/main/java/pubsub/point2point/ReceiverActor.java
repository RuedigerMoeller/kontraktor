package pubsub.point2point;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

/**
 * Created by moelrue on 28.09.2015.
 */
public class ReceiverActor extends Actor<ReceiverActor> {

    public IPromise receiveAsk(String topic, Object message) {
        return new Promise("dummy");
    }

    public void receiveTell(String topic, Object message) {
    }

}
