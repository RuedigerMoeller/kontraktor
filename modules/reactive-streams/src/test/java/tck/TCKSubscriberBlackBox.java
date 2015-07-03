package tck;

import org.nustaq.kontraktor.reactivestreams.ReaktiveStreams;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.tck.SubscriberBlackboxVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

/**
 * Created by ruedi on 03/07/15.
 */
@Test
public class TCKSubscriberBlackBox extends SubscriberBlackboxVerification<Long> {

    public TCKSubscriberBlackBox() {
        super(new TestEnvironment(300l));
    }

    @Override
    public Subscriber<Long> createSubscriber() {
        return ReaktiveStreams.get().subscriber( (res,err) ->  {
        });
    }

    @Override
    public Long createElement(int element) {
        return Long.valueOf(Integer.toString(element));
    }

    @Override
    public Publisher<Long> createHelperPublisher(long elements) {
        return null;
    }
}
