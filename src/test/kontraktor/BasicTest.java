package kontraktor;

import de.ruedigermoeller.kontraktor.Actor;
import de.ruedigermoeller.kontraktor.Actors;
import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.impl.*;
import org.junit.Test;

import java.util.concurrent.*;

import static org.junit.Assert.assertTrue;

/**
 * Created by ruedi on 06.05.14.
 */
public class BasicTest {

    public static class Bench extends Actor {
        protected int count;
        public void benchCall( String a, String b, String c) {
            count++;
        }
    }

    private long bench(Bench actorA) {
        long tim = System.currentTimeMillis();
        int numCalls = 1000 * 1000 * 10;
        for ( int i = 0; i < numCalls; i++ ) {
            actorA.benchCall("A", "B", "C");
        }
        actorA.getDispatcher().waitEmpty(1000*1000);
        final long l = (numCalls / (System.currentTimeMillis() - tim)) * 1000;
        System.out.println("tim "+ l +" calls per sec");
        return l;
    }

    @Test
    public void callBench() {
        Bench b = Actors.SpawnActor(Bench.class);
        bench(b);
        long callsPerSec = bench(b);
        b.stop();
        assertTrue(callsPerSec > 2 * 1000 * 1000);
    }

    public static class BenchSub extends Bench {
        @Override
        public void benchCall(String a, String b, String c) {
            super.benchCall(a, b, c);
        }
          
        public void getResult( Callback<Integer> cb ) {
            cb.receiveResult(count,null);
        }
    }

    @Test
    public void testInheritance() {
        final BenchSub bs = Actors.SpawnActor(BenchSub.class);
        for (int i : new int[10] ) {
            bs.benchCall("u", "o", null);
        }
        final CountDownLatch latch = new CountDownLatch(1);
        bs.getResult( new Callback<Integer>() {
            @Override
            public void receiveResult(Integer result, Object error) {
                assertTrue(result.intValue()==10);
                bs.stop();
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void lockStratTest() {
//        Executor ex = Executors.newCachedThreadPool();
//        for ( int iii : new int[3] ) {
//            ex.execute( new Runnable() {
//                @Override
//                public void run() {
//                    BackOffStrategy backOffStrategy = new BackOffStrategy();
//                    for (int i = 0; i < 1000; i++) {
//                        for (int ii = 0; ii < 160000; ii++) {
//                            backOffStrategy.yield(ii);
//                        }
//                        System.out.println("plop");
//                    }
//                }
//            });
//        }
//        try {
//            Thread.sleep(60000);
//        } catch (InterruptedException e) {
//            e.printStackTrace();
//        }
    }


}
