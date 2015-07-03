package tck;

import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

/**
 * Created by ruedi on 03/07/15.
 */
@Test
public class TCKSyncPubSyncL extends TCKSyncPubEventSink {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return ((EventSink)super.createPublisher(elements)).map( l -> l );
    }
}
