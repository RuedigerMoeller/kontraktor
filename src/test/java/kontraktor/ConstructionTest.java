package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.impl.SimpleScheduler;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 14.06.14.
 */
public class ConstructionTest {

    static AtomicInteger errors = new AtomicInteger(0);

    public static class ConstructionSampleActor extends Actor<ConstructionSampleActor> {

        public void $sameThread(Thread thread) {
            if ( thread != __currentDispatcher ) {
                errors.incrementAndGet();
            }
        }

        public void $notSameThread(Thread thread) {
            if ( thread == __currentDispatcher ) {
                errors.incrementAndGet();
            }
        }

        public void $test(int qSize, Scheduler sched) {
            if ( Thread.currentThread() != __currentDispatcher ) {
                errors.incrementAndGet();
            }
            if ( __mailboxCapacity != qSize ) {
                errors.incrementAndGet();
            }
            if ( __scheduler != sched )
                errors.incrementAndGet();
        }

    }


    @Test
    public void creationTest() throws InterruptedException {

        ConstructionSampleActor act = Actors.AsActor(ConstructionSampleActor.class);
        act.$test( SimpleScheduler.DEFQSIZE, act.__scheduler );
        ConstructionSampleActor act1 = Actors.AsActor(ConstructionSampleActor.class);
        act1.$sameThread(act1.__currentDispatcher);
        act1.$test( SimpleScheduler.DEFQSIZE, act1.__scheduler);

        SimpleScheduler scheduler = new SimpleScheduler(7000);
        ConstructionSampleActor act2 = Actors.AsActor(ConstructionSampleActor.class, scheduler);
        act2.$notSameThread(act.__currentDispatcher);
        act2.$test(8192,scheduler);

        ConstructionSampleActor act4 = Actors.AsActor(ConstructionSampleActor.class, scheduler, 60000);
        act4.$test(65536,scheduler);

        Thread.sleep(200);
        Assert.assertTrue(errors.get()==0);
    }

}
