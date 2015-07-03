package tck;

import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.PublisherVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

/**
 * Created by ruedi on 03/07/15.
 */
@Test
public class TCKSyncPubEventSink extends PublisherVerification {

    public TCKSyncPubEventSink() {
        super(new TestEnvironment(300L));
    }

    @Override
    public Publisher<Long> createPublisher(long elements) {
        EventSink<Long> sink = new EventSink<>();
        new Thread(() -> {
            for ( long i = 0; i < elements; i++ ) {
                while( ! sink.offer(i) ) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
            sink.complete();
        }, "feeder").start();
        return sink;
    }

    @Override
    public Publisher<Long> createFailedPublisher() {
        // Null because we always successfully subscribe.
        // If the observable is in error state, it will subscribe and then emit the error as the first item
        // This is not an “error state” publisher as defined by RS
        return null;
    }

}