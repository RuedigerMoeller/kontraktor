package newimpl;

import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.reallive.server.FilebasedRemoveLog;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class RemoveLogTest {

    @Test
    public void testRW() throws InterruptedException {
        FilebasedRemoveLog frl = Actors.AsActor( FilebasedRemoveLog.class );
        frl.init("/tmp/", "test");
        long now = System.currentTimeMillis();
        CountDownLatch latch = new CountDownLatch(1);
        new Thread( () -> {
            for (int i = 0; i < 5000; i++) {
                frl.add( now +i*10, "pok"+i);
            }
            frl.flush();
            System.out.println("flushed");
            frl.ping().await();
            latch.countDown();
        }).start();
        latch.await();
        AtomicInteger count = new AtomicInteger();
        System.out.println("query");
        frl.query( now-1, now+1000, (r,e) -> {
            if ( r != null ) {
                count.incrementAndGet();
//                System.out.println(r);
            } else {
                System.out.println(count+" matches");
            }
        });
        countEntries(frl, now);
        Thread.sleep(5_000l);
        frl.prune( 5000 );
        countEntries(frl, now);
    }

    private static void countEntries(FilebasedRemoveLog frl, long now) {
        AtomicInteger otherCount = new AtomicInteger();
        frl.query( 0, now +1000, (r, e) -> {
            if ( r != null ) {
                otherCount.incrementAndGet();
            } else {
                System.out.println(otherCount+" entries");
            }
        });
    }
}
