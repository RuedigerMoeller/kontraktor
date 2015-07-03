package tck;

import org.nustaq.kontraktor.reactivestreams.EventSink;
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
        // need to use ridiculous small batchsize here, don not use such small batch sizes
        // in an applciation 10k to 50k should be capable to overcome request(N) latency
        // and avoid the sender runnning dry
        return ReaktiveStreams.get().subscriber( 4, (res,err) ->  {
        });
    }

    @Override
    public Long createElement(int element) {
        return Long.valueOf(Integer.toString(element));
    }

    @Override
    public Publisher<Long> createHelperPublisher(long elements) {
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
}
