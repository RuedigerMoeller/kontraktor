package kontraktor;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertTrue;
import static org.nustaq.kontraktor.Actors.*;

/**
 * Created by ruedi on 25.03.2015.
 */
public class FutureCatch {

    public static class Generator {

        public volatile boolean fin = false;
        public Future in = new Promise<>();
        public Future out = new Promise<>();

        public Object next( Object o ) {
            System.out.println("next "+fin);
            if ( fin ) {
                return null;
            }
            in.resolve(o);
            Object res = out.await();
            System.out.println("out yielded");
            out = new Promise<>();
            return res;
        }

        public void run() {
            System.out.println("run");
            for ( int i = 0; i < 10; i++ ) {
                Future inPrev = in;
                in = new Promise<>();
                final int finalI = i;
                inPrev.then(r -> {
                    System.out.println("in yielded");
                    out.resolve(finalI);
                });
            }
            fin = true;
        }
    }

    public static class FutCatch extends Actor<FutCatch> {

        public void $generator() {
            Generator gen = new Generator();
            self().$run(() -> gen.run());
            int count = 0;
            while (!gen.fin) {
                System.out.println("GEN:" + gen.next("POK " + count++));
            }
        }

        public Future<String> $error(int num) {
            Promise res = new Promise();
            delayed(500, () -> res.settle(null, "Error " + num));
            return res;
        }

        public Future<String> $badex(int num) {
            Promise res = new Promise();
            delayed( 500, () -> res.settle(null, new Error("oh noes "+num)) );
            return res;
        }

        public Future<String> $result(int num) {
            Promise res = new Promise();
            delayed( 500, () -> res.settle("Result " + num, null) );
            return res;
        }

        public Future<String> $rand(int num) {
            Promise res = new Promise();
            delayed( 500+(long)(Math.random()*500), () -> res.settle("Result " + num, null) );
            return res;
        }

        public Future<Integer> $testESYield() {
            int correctCount = 0;
            AtomicInteger count = new AtomicInteger(0);
            try {
                System.out.println( self().$result(1).await() );
                System.out.println( self().$result(2).await() );
                System.out.println( self().$result(3).await() );
                System.out.println( self().$result(4).await() );
                System.out.println( self().$error(13).await() );
            } catch (Exception e) {
                correctCount++;
                e.printStackTrace();
            }
            try {
                System.out.println( self().$result(1).await() );
                System.out.println( self().$result(2).await() );
                System.out.println( self().$result(3).await() );
                System.out.println( self().$result(4).await() );
                System.out.println( self().$badex(17).await() );
            } catch (Throwable e) {
                correctCount++;
                e.printStackTrace();
            }
            stream( self().$rand(1),
                    self().$rand(2),
                    self().$rand(3),
                    self().$rand(4)
            ).forEach( r -> { System.out.print("," + r); count.incrementAndGet(); });

            String race = race(self().$rand(1),
                    self().$rand(2),
                    self().$rand(3),
                    self().$rand(4)
            ).await();
            System.out.println();
            System.out.println("rc "+race);
            return new Promise<>(correctCount+count.get());
        }
    }

//    @Test
    public void testGen() {
        FutCatch futCatch = AsActor(FutCatch.class);
        futCatch.$generator();
        try {
            Thread.sleep(100000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Test
    public void testTimeout() {
        FutCatch futCatch = AsActor(FutCatch.class);
        int suc = 0;
        try {
            futCatch.$result(12).timeoutIn(100).await();
        } catch ( Throwable t ) {
            t.printStackTrace();
            suc++;
        }
        try {
            futCatch.$result(12).timeoutIn(1000).await();
        } catch ( Throwable t ) {
            t.printStackTrace();
            suc++;
        }
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(suc == 1);
        futCatch.$stop();
    }

    @Test
    public void testESY() {
        final FutCatch futCatch = AsActor(FutCatch.class);
        Integer sync = futCatch.$testESYield().await();
        System.out.println("Done");
        assertTrue(sync.intValue() == 6);
        futCatch.$stop();
    }

    @Test
    public void testFut() throws InterruptedException {
        AtomicBoolean res = new AtomicBoolean(true);
        final FutCatch futCatch = AsActor(FutCatch.class);
        AtomicInteger count = new AtomicInteger(0);

        futCatch.$result(0)
           .then( r -> {
               System.out.println("" + r);
               count.addAndGet(1);
               return futCatch.$result(1);
           })
           .then(r -> {
               System.out.println("" + r);
               count.addAndGet(2);
               return futCatch.$result(2);
           })
           .then(r -> {
               System.out.println("" + r);
               count.addAndGet(3);
               return futCatch.$error(1);
           })
           .then(r -> {
               System.out.println("" + r);
               count.addAndGet(4);
               return futCatch.$result(3);
           })
           .then(r -> {
               System.out.println("" + r);
               count.addAndGet(5);
               return futCatch.$result(4);
           })
           .catchError(error -> {
               count.addAndGet(5);
               System.out.println("catched " + error);
           })
           .then((r, e) -> System.out.println("done "+count.get()))
           .await();

        assertTrue(count.get() == 11);
        futCatch.$stop();
    }

}
