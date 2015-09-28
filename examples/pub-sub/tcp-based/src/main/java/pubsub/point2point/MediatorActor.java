package pubsub.point2point;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.util.Log;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by moelrue on 28.09.2015.
 *
 * This one acts like a message queue / gossip server. All members of pub sub
 * register and publish to this actor.
 *
 */
public class MediatorActor extends Actor<MediatorActor> {

    HashMap<String,List<ReceiverActor>> topic2Subscriber;

    public void init() {
        topic2Subscriber = new HashMap<>();
    }

    public void subscribe( String topic,  ReceiverActor receiver ) {
        List<ReceiverActor> receiverActors = topic2Subscriber.get(topic);
        if ( receiverActors == null) {
            receiverActors = new ArrayList<>();
            topic2Subscriber.put(topic,receiverActors);
        }
        receiverActors.add(receiver);
    }

    public void unsubscribeAll( ReceiverActor subsriber ) {
        topic2Subscriber.forEach( (topic,list) -> unsubscribe(topic,subsriber));
    }

    public void unsubscribe( String topic, ReceiverActor subsriber ) {
        List<ReceiverActor> receiverActors = topic2Subscriber.get(topic);
        if ( receiverActors != null ) {
            for (Iterator<ReceiverActor> iterator = receiverActors.iterator(); iterator.hasNext(); ) {
                ReceiverActor receiverActor = iterator.next();
                if ( receiverActor.equals(subsriber) ) // do not receive self sent
                    iterator.remove();
            }
        }
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
     * send a message to all and stream the result of each receiver back to sender
     *
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
                all((List) results).await(5000); // is non-blocking
            } catch (Exception ex) {
                // timeout goes here
                Log.Info(this, "timeout in broadcast");
            }
            results.forEach( promise -> cb.stream(promise.get()));
            cb.finish(); // important to release callback mapping in remoting !
        }
    }

    public static void main(String[] args) {
        MediatorActor mediator = Actors.AsActor(MediatorActor.class);
        mediator.init();
        new WebSocketPublisher(mediator, "localhost", "/", 9090 )
            .serType(SerializerType.FSTSer)
            .publish((disconnectedActor) -> {
                Log.Info(null, "connection lost " + disconnectedActor);
                mediator.unsubscribeAll((ReceiverActor) disconnectedActor);
            });
    }

}
