package tck;

import org.nustaq.kontraktor.reactivestreams.ReaktiveStreams;
import org.reactivestreams.Processor;
import org.reactivestreams.Publisher;
import org.reactivestreams.tck.IdentityProcessorVerification;
import org.reactivestreams.tck.TestEnvironment;
import org.testng.annotations.Test;

import java.util.concurrent.ExecutorService;

/**
 * Created by ruedi on 03/07/15.
 */
@Test
public class IdentityProcessorAsync extends IdentityProcessorVerification<Long> {

    public static final long DEFAULT_TIMEOUT_MILLIS = 300L;
    public static final long PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS = 1000L;


    public IdentityProcessorAsync() {
        super(new TestEnvironment(DEFAULT_TIMEOUT_MILLIS), PUBLISHER_REFERENCE_CLEANUP_TIMEOUT_MILLIS);
    }

    @Override
    public ExecutorService publisherExecutorService() {
        return null;
    }

    @Override
    public Long createElement(int element) {
        return Long.valueOf(element);
    }

    @Override
    public Processor<Long, Long> createIdentityProcessor(int bufferSize) {
        return ReaktiveStreams.get().newAsyncProcessor(i -> i, bufferSize);
    }

    @Override
    public Publisher<Long> createHelperPublisher(long elements) {
        return TCKSyncPubEventSink.createRangePublisher(elements);
    }

    // ENABLE ADDITIONAL TESTS

    @Override
    public Publisher<Long> createFailedPublisher() {
        // return Publisher that only signals onError instead of null to run additional tests
        // see this methods JavaDocs for more details on how the returned Publisher should work.
        return null;
    }

    // OPTIONAL CONFIGURATION OVERRIDES
    // only override these if understanding the implications of doing so.

    @Override
    public long maxElementsFromPublisher() {
        return super.maxElementsFromPublisher();
    }

    @Override
    public long boundedDepthOfOnNextAndRequestRecursion() {
        return super.boundedDepthOfOnNextAndRequestRecursion();
    }

    // disabled as accept multiple subscribers
    public void required_spec205_mustCallSubscriptionCancelIfItAlreadyHasAnSubscriptionAndReceivesAnotherOnSubscribeSignal() {}

    // white box subscriber test not implementatble without changing source of tested software
    public void required_exerciseWhiteboxHappyPath() {}

    // multi subscriber always waits for slowest subscriber, in this testcase only one of several subscribers
    // receives a request(N), the other one not such that messages are buffered and not sent.
    public void required_mustRequestFromUpstreamForElementsThatHaveBeenRequestedLongAgo() {}

    // same issue as above
    public void required_spec104_mustCallOnErrorOnAllItsSubscribersIfItEncountersANonRecoverableError() {}
}