package org.newstaq.kontraktor.remoting.aeron;

import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.common.CommonContext;
import uk.co.real_logic.aeron.driver.Configuration;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.aeron.driver.NonBlockingFlowControl;
import uk.co.real_logic.agrona.CloseHelper;
import uk.co.real_logic.agrona.concurrent.UnsafeBuffer;

import java.nio.ByteBuffer;
import java.util.concurrent.TimeUnit;

import static uk.co.real_logic.aeron.common.CommonContext.ADMIN_DIR_PROP_NAME;
import static uk.co.real_logic.aeron.common.CommonContext.COUNTERS_DIR_PROP_NAME;
import static uk.co.real_logic.aeron.common.CommonContext.DATA_DIR_PROP_NAME;

/**
 * Created by moelrue on 11/24/14.
 */
public class BasicPublisher
{
    static final int STREAM_ID = 10;
    static final long NUMBER_OF_MESSAGES = 1_000_000;
    static final long LINGER_TIMEOUT_MS = 5000;

    static final boolean EMBEDDED_MEDIA_DRIVER = true;

    static final UnsafeBuffer BUFFER = new UnsafeBuffer(ByteBuffer.allocateDirect(256));
    static final int FRAGMENT_COUNT_LIMIT = 10;

    public static void useSharedMemoryOnLinux()
    {
        if ("Linux".equalsIgnoreCase(System.getProperty("os.name")))
        {
            if (null == System.getProperty(CommonContext.ADMIN_DIR_PROP_NAME))
            {
                System.setProperty(CommonContext.ADMIN_DIR_PROP_NAME, "/dev/shm/aeron/conductor");
            }

            if (null == System.getProperty(CommonContext.COUNTERS_DIR_PROP_NAME))
            {
                System.setProperty(CommonContext.COUNTERS_DIR_PROP_NAME, "/dev/shm/aeron/counters");
            }

            if (null == System.getProperty(CommonContext.DATA_DIR_PROP_NAME))
            {
                System.setProperty(CommonContext.DATA_DIR_PROP_NAME, "/dev/shm/aeron/data");
            }
        }
    }

    public static void main(final String[] args) throws Exception
    {
        System.setProperty(Configuration.SENDER_MULTICAST_FLOW_CONTROL_STRATEGY_PROP_NAME, NonBlockingFlowControl.class.getName() );
        String CHANNEL = "udp://localhost@224.10.9.9:40123";
        System.out.println("Publishing to " + CHANNEL + " on stream Id " + STREAM_ID);

//        useSharedMemoryOnLinux();

        final MediaDriver driver = EMBEDDED_MEDIA_DRIVER ? MediaDriver.launch() : null;
        final Aeron.Context ctx = new Aeron.Context();

        try (final Aeron aeron = Aeron.connect(ctx);
             final Publication publication = aeron.addPublication(CHANNEL, STREAM_ID))
        {
            for (int i = 0; i < NUMBER_OF_MESSAGES; i++)
            {
                final String message = "Hello World! " + i;
                BUFFER.putBytes(0, message.getBytes());

                System.out.print("offering " + i + "/" + NUMBER_OF_MESSAGES);
                final boolean result = publication.offer(BUFFER, 0, message.getBytes().length);

                if (!result)
                {
                    System.out.println(" ah?!");
                }
                else
                {
                    System.out.println(" yay!");
                }

                Thread.sleep(1);
            }

            System.out.println("Done sending.");

            if (0 < LINGER_TIMEOUT_MS)
            {
                System.out.println("Lingering for " + LINGER_TIMEOUT_MS + " milliseconds...");
                Thread.sleep(LINGER_TIMEOUT_MS);
            }
        }

        CloseHelper.quietClose(driver);
    }
}
