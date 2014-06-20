package kontraktor;

import io.jaq.mpsc.MpscConcurrentQueue;
import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.impl.ElasticScheduler;

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
            if ( ((MpscConcurrentQueue)__mailbox).getCapacity() != qSize ) {
                errors.incrementAndGet();
            }
            if ( __scheduler != sched )
                errors.incrementAndGet();
        }

    }


    @Test
    public void creationTest() throws InterruptedException {

        ConstructionSampleActor act = Actors.AsActor(ConstructionSampleActor.class);
        act.$test( ElasticScheduler.DEFQSIZE, Actors.instance.__testGetScheduler() );
        ConstructionSampleActor act1 = Actors.AsActor(ConstructionSampleActor.class);
        act1.$sameThread(act.__currentDispatcher);
        act1.$test( ElasticScheduler.DEFQSIZE, act.__scheduler);

        ElasticScheduler scheduler = new ElasticScheduler(1, 7000);
        ConstructionSampleActor act2 = Actors.AsActor(ConstructionSampleActor.class, scheduler);
        act2.$notSameThread(act.__currentDispatcher);
        act2.$test(8192,scheduler);

        ConstructionSampleActor act4 = Actors.AsActor(ConstructionSampleActor.class, scheduler, 60000);
        act4.$test(65536,scheduler);

        Thread.sleep(200);
        Assert.assertTrue(errors.get()==0);
    }

}
