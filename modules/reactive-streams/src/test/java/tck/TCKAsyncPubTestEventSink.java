package tck;

import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;

/**
 * Created by ruedi on 03/07/15.
 */
@org.testng.annotations.Test
public class TCKAsyncPubTestEventSink extends PublisherVerification<Long> {

    public TCKAsyncPubTestEventSink() {
        super(new TestEnvironment(300L));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return TCKSyncPubEventSink.createRangePublisher(elements);
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        return null;
    }

}
