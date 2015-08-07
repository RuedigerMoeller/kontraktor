package kontraktor;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.impl.DispatcherThread;
import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 07/08/15.
 */
public class LifeCycleTest {

    public static class LCycle extends Actor<LCycle> {

        LCycle1 lc;

        public void init1() {
            lc = Actors.AsActor(LCycle1.class);
        }

        @Override
        public void stop() {
            lc.stop();
            super.stop();
        }

        public void init2() {
            lc = Actors.AsActor(LCycle1.class,getScheduler());
        }

    }

    public static class LCycle1 extends Actor<LCycle1> {

    }

    @Test
    public void startStop() throws InterruptedException {
        Log.Info(this,"XX");
        int threads = DispatcherThread.activeDispatchers.get();
        System.out.println("Active:" + threads);

        LCycle lCycle = Actors.AsActor(LCycle.class);
//        lCycle.getScheduler().terminateIfIdle();
        lCycle.init1();

        lCycle.stop();

        Thread.sleep(10000);
        System.out.println("Active:" + DispatcherThread.activeDispatchers.get() );
        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == threads);
    }

    @Test
    public void startStopSingleThread() throws InterruptedException {
        Log.Info(this,"XX");
        int threads = DispatcherThread.activeDispatchers.get();
        System.out.println("Active:" + threads);

        LCycle lCycle = Actors.AsActor(LCycle.class);
//        lCycle.getScheduler().terminateIfIdle();
        lCycle.init2();

        lCycle.stop();

        Thread.sleep(10000);
        System.out.println("Active:" + DispatcherThread.activeDispatchers.get() );
        Assert.assertTrue(DispatcherThread.activeDispatchers.get() == threads);
    }

}
