package kontraktor;

import de.ruedigermoeller.kontraktor.*;
import de.ruedigermoeller.kontraktor.annotations.*;
import kontraktor.BasicTest.ServiceActor.*;
import org.junit.Test;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

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
        final long l = (numCalls / (System.currentTimeMillis() - tim)) * 1000;
        System.out.println("tim "+ l +" calls per sec");
        actorA.getDispatcher().waitEmpty(3000*1000);
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

        public static interface DataAccess {
            HashMap getMap();
        }

        HashMap myPrivateData;

        public void init() {
            myPrivateData = new HashMap();
            myPrivateData.put("One", "Two");
            myPrivateData.put(3, 4);
            myPrivateData.put("five", 6);
        }

        public void getString( SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

        public void getStringAnnotated( @InThread SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

        @Override
        protected Object getActorAccess() {
            return new DataAccess() {
                @Override
                public HashMap getMap() {
                    return myPrivateData;
                }
            };
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

            service.executeInActorThread(
                    new ActorRunnable() {
                        @Override
                        public void run(Object actorAccess, Actor actorImpl, Callback resultReceiver) {
                            if ( service.getDispatcher() == Thread.currentThread() ) {
                                success++;
                            } else {
                                System.out.println("POKPOK err");
                            }
                            DataAccess access = (DataAccess) actorAccess;
                            Iterator iterator = access.getMap().keySet().iterator();
                            while( iterator.hasNext() ) {
                                Object o = iterator.next();
                                if ( "five".equals(o) ) {
                                    resultReceiver.receiveResult(access.getMap().get(o),null);
                                }
                            }
                        }
                    },
                    new Callback() {
                        @Override
                        public void receiveResult(Object result, Object error) {
                            if (callerThread != Thread.currentThread()) {
                                throw new RuntimeException("Dammit");
                            } else {
                                success++;
                                System.out.println("Alles prima 2");
                            }
                            System.out.println("res "+result);
                        }
                    }
            );

        }
    }

    @Test
    public void inThreadTest() throws InterruptedException {
        ServiceActor service = AsActor(ServiceActor.class);
        service.init();

        MyActor cbActor = AsActor(MyActor.class);
        cbActor.init(service);
        cbActor.callbackTest();

        Thread.sleep(1000);
        cbActor.stop();
        assertTrue(((MyActor)cbActor.getActor()).success == 4);
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


    static AtomicInteger yieldThreadErrors = new AtomicInteger(0);
    public static class YieldCallerActor extends Actor {

        YieldService service;
        int state = 0;

        public void init() {
            service = Actors.SpawnActor(YieldService.class);
        }

        public void yieldException() {
            try {
                service.yield().yieldException();
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            yieldThreadErrors.incrementAndGet();
        }

        public void mainLoop(int count) {
            if ( count == 0 ) {
                return;
            }
            final int preState = state;

            String aResult = service.yield().getSomething(state, Thread.currentThread());
            System.out.println("state post call "+state+" pre:"+preState+" res "+aResult);

            state++;
            LockSupport.parkNanos(1000 * 1000 * 1); // simulate processing, yielded call is slower
            self().mainLoop(count-1);
        }

        @Override
        protected YieldCallerActor self() {
            return super.self();
        }

        public Integer getState() {
            return state;
        }

        public void changeState() {
            state++;
        }

        @Override
        public YieldCallerActor yield() {
            return super.yield();
        }
    }

    public static class YieldService extends Actor {

        Thread firstDispatcher;
        public String getSomething( int i, Thread caller ) {
            if ( firstDispatcher == null ) {
                firstDispatcher = Thread.currentThread();
            } else {
                if ( firstDispatcher != Thread.currentThread() ) {
                    yieldThreadErrors.incrementAndGet();
                }
                if ( caller == Thread.currentThread() ) {
                    yieldThreadErrors.incrementAndGet();
                }
            }
            LockSupport.parkNanos(1000*1000*10); // simulate processing
            return "result"+i;
        }

        public String yieldException() {
            throw new RuntimeException("Bla");
        }

        @Override
        public YieldService yield() {
            return super.yield();
        }
    }

    @Test
    public void yieldTest() {
        YieldCallerActor actor = Actors.SpawnActor(YieldCallerActor.class);
        actor.init();
        boolean hadEx = false;

        actor.yieldException();

        actor.mainLoop(5000);
        for (int i = 0; i<10000; i++) {
            actor.changeState();
            LockSupport.parkNanos(1000 * 1000); // simulate processing
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("Thread errors "+yieldThreadErrors.get());
        assertTrue(yieldThreadErrors.get()==0);
        assertTrue(actor.yield().getState()!=0);
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
