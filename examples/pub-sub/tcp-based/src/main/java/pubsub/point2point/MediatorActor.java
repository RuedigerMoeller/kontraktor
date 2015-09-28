package pubsub.point2point;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.util.Log;

import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by moelrue on 28.09.2015.
 */
public class MediatorActor extends Actor<MediatorActor> {

    HashMap<String,List<ReceiverActor>> topic2Subscriber;

    public void init() {
        topic2Subscriber = new HashMap<>();
    }

    /**
     * send a fire and forget message to all
     *
     * @param sender
     * @param topic
     * @param message
     */
    public void tellSubscribers( Actor sender, String topic, Object message) {
        List<ReceiverActor> subscriber = topic2Subscriber.get(topic);
        if ( subscriber != null ) {
            subscriber.stream()
                .filter(subs -> !subs.equals(sender))
                .forEach(subs -> subs.receiveTell(topic, message) );
        }
    }

    /**
     * send a message to all and stream each result
     * @param sender
     * @param topic
     * @param message
     * @param cb
     */
    public void askSubscribers( Actor sender, String topic, Object message, Callback cb ) {
        List<ReceiverActor> subscriber = topic2Subscriber.get(topic);
        if ( subscriber != null ) {
            List<IPromise> results = subscriber.stream()
                    .filter(subs -> !subs.equals(sender))
                    .map(subs -> subs.receiveAsk(topic, message))
                    .collect(Collectors.toList());
            try {
                all((List) results).await(); // is non-blocking
            } catch (Exception ex) {
                // timeout goes here
                Log.Info(this, "timeout in broadcast");
            }
            results.forEach( promise -> cb.stream(promise.get()));
            cb.finish(); // important to release callback mapping in remoting !
        }
    }

}
