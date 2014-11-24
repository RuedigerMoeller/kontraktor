package org.newstaq.kontraktor.remoting.aeron;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Subscription;
import uk.co.real_logic.aeron.common.BackoffIdleStrategy;
import uk.co.real_logic.aeron.common.IdleStrategy;
import uk.co.real_logic.aeron.common.concurrent.SigInt;
import uk.co.real_logic.aeron.common.concurrent.logbuffer.DataHandler;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.agrona.CloseHelper;

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

    public static DataHandler printStringMessage(final int streamId)
    {
        return (buffer, offset, length, header) ->
        {
            final byte[] data = new byte[length];
            buffer.getBytes(offset, data);

            System.out.println(
                    String.format(
                            "message to stream %d from session %x (%d@%d) <<%s>>",
                            streamId, header.sessionId(), length, offset, new String(data)));
        };
    }

    public static Consumer<Subscription> subscriberLoop(final int limit, final AtomicBoolean running)
    {
        final IdleStrategy idleStrategy = new BackoffIdleStrategy(
                100, 10, TimeUnit.MICROSECONDS.toNanos(1), TimeUnit.MICROSECONDS.toNanos(100));

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
        System.setProperty("java.net.preferIPv4Stack","true" );
        String channel = "udp://localhost@224.10.9.9:40123";
        System.out.println("Subscribing to " + channel + " on stream Id " + BasicPublisher.STREAM_ID);

//        BasicPublisher.useSharedMemoryOnLinux();

        final MediaDriver driver = BasicPublisher.EMBEDDED_MEDIA_DRIVER ? MediaDriver.launch() : null;

        final Aeron.Context ctx = new Aeron.Context()
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
