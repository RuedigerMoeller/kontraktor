package org.newstaq.kontraktor.remoting.aeron;

import org.nustaq.fastcast.util.RateMeasure;
import org.nustaq.fastcast.util.Sleeper;
import org.nustaq.offheap.bytez.Bytez;
import org.nustaq.offheap.bytez.malloc.MallocBytez;
import org.nustaq.offheap.bytez.onheap.HeapBytez;
import org.nustaq.offheap.structs.FSTStruct;
import org.nustaq.offheap.structs.FSTStructAllocator;
import org.nustaq.offheap.structs.structtypes.StructString;
import org.nustaq.offheap.structs.unsafeimpl.FSTStructFactory;
import uk.co.real_logic.aeron.Aeron;
import uk.co.real_logic.aeron.Publication;
import uk.co.real_logic.aeron.common.BackoffIdleStrategy;
import uk.co.real_logic.aeron.common.BusySpinIdleStrategy;
import uk.co.real_logic.aeron.common.CommonContext;
import uk.co.real_logic.aeron.common.IdleStrategy;
import uk.co.real_logic.aeron.driver.Configuration;
import uk.co.real_logic.aeron.driver.MediaDriver;
import uk.co.real_logic.aeron.driver.NonBlockingFlowControl;
import uk.co.real_logic.aeron.driver.ThreadingMode;
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

    // gets instrumented don't add logic to methods ..
    public static class TestMsg extends FSTStruct {

        protected StructString string = new StructString(15);
        protected long timeNanos;

        public StructString getString() {
            return string;
        }

        public void setString(StructString string) {
            this.string = string;
        }

        public long getTimeNanos() {
            return timeNanos;
        }

        public void setTimeNanos(long timeNanos) {
            this.timeNanos = timeNanos;
        }
    }


    static final int STREAM_ID = 10;
    static final long NUMBER_OF_MESSAGES = 100_000_000;
    static final long LINGER_TIMEOUT_MS = 5000;

    static final UnsafeBuffer BUFFER = new UnsafeBuffer(ByteBuffer.allocateDirect(256));
    static final int FRAGMENT_COUNT_LIMIT = 10;

    static final MediaDriver.Context mctx = new MediaDriver.Context()
            .threadingMode(ThreadingMode.DEDICATED)
            .conductorIdleStrategy(new BackoffIdleStrategy(1, 1, 1, 1))
            .sharedNetworkIdleStrategy(new BusySpinIdleStrategy())
            .sharedIdleStrategy(new BusySpinIdleStrategy())
            .receiverIdleStrategy(new BusySpinIdleStrategy())
            .senderIdleStrategy(new BusySpinIdleStrategy());

    public static void main(final String[] args) throws Exception
    {
//        System.setProperty(Configuration.MTU_LENGTH_PROP_NAME, "1496" );
        String CHANNEL = "udp://127.0.0.1@224.10.9.9:40123";
        System.out.println("Publishing to " + CHANNEL + " on stream Id " + STREAM_ID);

        FSTStructFactory.getInstance().registerClz(TestMsg.class);
        TestMsg template = new TestMsg();
        FSTStructAllocator allocator = new FSTStructAllocator(0);

        TestMsg toSend = allocator.newStruct(template);
        byte[] base = ((HeapBytez) toSend.getBase()).getBase();

//        final MediaDriver driver = MediaDriver.launch(mctx);
        final MediaDriver driver = MediaDriver.launch();
        final Aeron.Context ctx = new Aeron.Context();

        Sleeper sleeper = new Sleeper();
        RateMeasure measure = new RateMeasure("msg send");
//        try (final Aeron aeron = Aeron.connect(ctx.idleStrategy(new BusySpinIdleStrategy()));
        try (final Aeron aeron = Aeron.connect(ctx);
             final Publication publication = aeron.addPublication(CHANNEL, STREAM_ID))
        {
            for (int i = 0; i < NUMBER_OF_MESSAGES; /**i++**/)
            {
                toSend.setTimeNanos(System.nanoTime());
                BUFFER.putBytes(0, base, 0, toSend.getByteSize() );
                final boolean result = publication.offer(BUFFER, 0, toSend.getByteSize());
                if (!result)
                {

                }
                else
                {
                    measure.count();
                }
//                sleeper.sleepMicros(50);
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
