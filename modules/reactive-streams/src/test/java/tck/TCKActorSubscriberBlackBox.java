package tck;

import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.reactivestreams.Processor;
import org.reactivestreams.Subscriber;
import org.testng.annotations.Test;

/**
 * Created by ruedi on 03/07/15.
 */
@Test
public class TCKActorSubscriberBlackBox extends TCKSubscriberBlackBox {


    @Override
    public Subscriber<Long> createSubscriber() {
        Processor<Long, Long> longLongProcessor = KxReactiveStreams.get().<Long, Long>newAsyncProcessor(l -> l,4);
        longLongProcessor.subscribe(super.createSubscriber());
        return longLongProcessor;
    }

    // can handle multiple subscriptions
    public void required_spec205_blackbox_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() {}
}
