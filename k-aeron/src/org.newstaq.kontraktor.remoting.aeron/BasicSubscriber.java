package org.newstaq.kontraktor.remoting.aeron;

import org.nustaq.offheap.bytez.onheap.HeapBytez;
import org.nustaq.offheap.structs.unsafeimpl.FSTStructFactory;
import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.BackoffIdleStrategy;
import uk.co.real_logic.aeron.common.BusySpinIdleStrategy;
import uk.co.real_logic.aeron.common.IdleStrategy;
import uk.co.real_logic.aeron.common.concurrent.SigInt;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.DataHandler;
import uk.co.real_logic.aeron.driver.Configuration;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.agrona.CloseHelper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

/**
 * Created by moelrue on 11/24/14.
 */
public class BasicSubscriber {

    public static void printNewConnection(
            final String channel, final int streamId, final int sessionId, final String sourceInformation)
    {
        System.out.println(
                String.format(
                        "new connection on %s streamId %d sessionId %x from %s",
                        channel, streamId, sessionId, sourceInformation));
    }

    public static void printInactiveConnection(final String channel, final int streamId, final int sessionId)
    {
        System.out.println(
                String.format(
                        "inactive connection on %s streamId %d sessionId %x",
                        channel, streamId, sessionId));
    }


    static final Executor worker = Executors.newSingleThreadExecutor();

    static int count = 0;
    static final byte[] b = new byte[80000];
    static HeapBytez hp = new HeapBytez(b);
    public static DataHandler printStringMessage(final int streamId)
    {
        return (buffer, offset, length, header) ->
        {
            buffer.getBytes(offset, b);
            BasicPublisher.TestMsg received = FSTStructFactory.getInstance().getStructPointer(hp, 0).cast();
            final long nanos = System.nanoTime() - received.getTimeNanos();
            if (count++ % 1000 == 0) {
                final BasicPublisher.TestMsg finRec = received.detach();
                worker.execute(new Runnable() {
                    @Override
                    public void run() {
                        System.out.println("receive " + finRec.getString().toString() + " latency:" + (nanos / 1000));
                    }
                });
            }
        };
    }

    public static Consumer<Subscription> subscriberLoop(final int limit, final AtomicBoolean running)
    {
        final IdleStrategy idleStrategy = new BackoffIdleStrategy(
                1000000, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));

        return subscriberLoop(limit, running, idleStrategy);
    }

    public static Consumer<Subscription> subscriberLoop(
            final int limit, final AtomicBoolean running, final IdleStrategy idleStrategy)
    {
        return
                (subscription) ->
                {
                    try
                    {
                        while (running.get())
                        {
                            final int fragmentsRead = subscription.poll(limit);
                            idleStrategy.idle(fragmentsRead);
                        }
                    }
                    catch (final Exception ex)
                    {
                        ex.printStackTrace();
                    }
                };
    }

    public static void main(final String[] args) throws Exception
    {
        System.setProperty(Configuration.MTU_LENGTH_PROP_NAME, "1496" );
        BasicPublisher.useSharedMemoryOnLinux();

        FSTStructFactory.getInstance().registerClz(BasicPublisher.TestMsg.class);

        String channel = "udp://127.0.0.1@224.10.9.9:40123";
        System.out.println("Subscribing to " + channel + " on stream Id " + BasicPublisher.STREAM_ID);

        final MediaDriver driver = MediaDriver.launch(BasicPublisher.mctx);
//        final MediaDriver driver = MediaDriver.launch();

        final Aeron.Context ctx = new Aeron.Context()
                .idleStrategy(new BusySpinIdleStrategy())
                .newConnectionHandler(BasicSubscriber::printNewConnection)
                .inactiveConnectionHandler(BasicSubscriber::printInactiveConnection);

        final DataHandler dataHandler = printStringMessage(BasicPublisher.STREAM_ID);

        final AtomicBoolean running = new AtomicBoolean(true);
        SigInt.register(() -> running.set(false));

        try (final Aeron aeron = Aeron.connect(ctx);
             final Subscription subscription = aeron.addSubscription(channel, BasicPublisher.STREAM_ID, dataHandler))
        {
            // run the subscriber thread from here
            subscriberLoop(BasicPublisher.FRAGMENT_COUNT_LIMIT, running).accept(subscription);

            System.out.println("Shutting down...");
        }

        CloseHelper.quietClose(driver);
    }

}
