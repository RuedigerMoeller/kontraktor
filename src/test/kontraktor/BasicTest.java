package kontraktor;

import de.ruedigermoeller.kontraktor.Actor;
import de.ruedigermoeller.kontraktor.Actors;
import de.ruedigermoeller.kontraktor.Callback;
import de.ruedigermoeller.kontraktor.annotations.*;
import de.ruedigermoeller.kontraktor.impl.*;
import org.junit.Test;

import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static de.ruedigermoeller.kontraktor.Actors.*;
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
        Bench b = SpawnActor(Bench.class);
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
        final BenchSub bs = SpawnActor(BenchSub.class);
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

    public static interface SomeCallbackHandler {
        public void callbackReceived( Object callback );
    }

    public static class ServiceActor extends Actor {

        public void getString( SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

        public void getStringAnnotated( @InThread SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

    }

    public static class MyActor extends Actor {

        ServiceActor service;
        volatile int success = 0;

        public void init(ServiceActor service) {
            this.service = service;
        }

        public void callbackTest() {
            final Thread callerThread = Thread.currentThread();
            service.getString(InThread(new SomeCallbackHandler() {
                @Override
                public void callbackReceived(Object callback) {
                    if (callerThread != Thread.currentThread()) {
                        throw new RuntimeException("Dammit");
                    } else {
                        success++;
                        System.out.println("Alles prima");
                    }
                }
            }));
            service.getStringAnnotated(new SomeCallbackHandler() {
                @Override
                public void callbackReceived(Object callback) {
                    if (callerThread != Thread.currentThread()) {
                        throw new RuntimeException("Dammit 1");
                    } else {
                        success++;
                        System.out.println("Alles prima 1");
                    }
                }
            });
        }

    }

    @Test
    public void inThreadTest() throws InterruptedException {
        ServiceActor service = AsActor(ServiceActor.class);
        MyActor cbActor = AsActor(MyActor.class);
        cbActor.init(service);
        cbActor.callbackTest();
        Thread.sleep(1000);
        cbActor.stop();
        assertTrue(((MyActor)cbActor.getActor()).success == 2);
        service.stop();
    }

    public static class Overload extends Actor {

        public void a(int x, Callback<Integer> cb) {
            cb.receiveResult(x,null);
        }

        public void a(int x, int y, Callback<Integer> cb) {
            cb.receiveResult(y,null);
        }

    }

    @Test
    public void testOverload() {
        try {
            // verify exception is thrown
            Overload ov = AsActor(Overload.class);
            assertTrue(false);
        } catch (Exception e) {

        }
    }


    public static class TestBlockingAPI extends Actor {

        public void get( final String url, final Callback<String> content ) {
            final Thread myThread = getDispatcher();
            Actors.Execute(
                    new Callable<String>() {
                        @Override
                        public String call() throws Exception {
                            return new Scanner( new URL(url).openStream(), "UTF-8" ).useDelimiter("\\A").next();
                        }
                    },
                    new Callback<String>() {
                        @Override
                        public void receiveResult(String result, Object error) {
                            if ( Thread.currentThread() == myThread ) {
                                content.receiveResult(result,null);
                            } else {
                                content.receiveResult(null, "wrong thread");
                            }
                        }
                });
        }

    }

    @Test
    public void testBlockingCall() {
        final AtomicInteger success = new AtomicInteger(0);
        TestBlockingAPI actor = AsActor(TestBlockingAPI.class);
        actor.get("http://www.google.com", new Callback<String>() {
            @Override
            public void receiveResult(String result, Object error) {
                if ( error != null )
                    success.set(1);
                else
                    success.set(2);
            }
        });
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(success.get()!=1); // if no response (proxy etc) also return true
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
