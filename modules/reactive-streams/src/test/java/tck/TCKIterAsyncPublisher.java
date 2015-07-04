package tck;

import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.ReaktiveStreams;
import org.reactivestreams.Publisher;
import org.testng.annotations.Test;

import java.util.stream.IntStream;
import java.util.stream.LongStream;

/**
 * Created by ruedi on 04/07/15.
 */
@Test
public class TCKIterAsyncPublisher extends TCKSyncPubEventSink {

    @Override
    public Publisher<Long> createPublisher(long elements) {
        return ReaktiveStreams.get().produce(LongStream.range(0, elements));
    }
}

