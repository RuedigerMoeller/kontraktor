package kontraktor;

import org.junit.Ignore;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.annotations.*;
import org.nustaq.kontraktor.Promise;
import org.junit.Test;

import java.net.URL;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.nustaq.kontraktor.Actors.*;
import static org.junit.Assert.assertTrue;

/**
 * Created by ruedi on 06.05.14.
 */
public class BasicTest {

    public static class Bench extends Actor<Bench> {
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
        try {
            Thread.sleep(3 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return l;
    }

    @Test
    public void callBench() {
        Bench b = AsActor(Bench.class);
        bench(b);
        long callsPerSec = bench(b);
        b.$stop();
//        assertTrue(callsPerSec > 1 * 1000 * 1000);
    }

    public static class BenchSub extends Bench {
        @Override
        public void benchCall(String a, String b, String c) {
            super.benchCall(a, b, c);
        }
          
        public void getResult( Callback<Integer> cb ) {
            cb.receive(count, null);
        }
    }

    @Test
    public void testInheritance() {
        final BenchSub bs = AsActor(BenchSub.class);
        for (int i : new int[10] ) {
            bs.benchCall("u", "o", null);
        }
        final CountDownLatch latch = new CountDownLatch(1);
        bs.getResult( new Callback<Integer>() {
            @Override
            public void receive(Integer result, Object error) {
                assertTrue(result.intValue()==10);
                bs.$stop();
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

    public static class ServiceActor extends Actor<ServiceActor> {

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

        public Future<String> concat(String other) {
            return new Promise<>("Hallo"+other);
        }

        public void getStringAnnotated( @InThread SomeCallbackHandler callback ) {
            callback.callbackReceived("Hallo");
        }

    }

    public static class MyActor extends Actor<MyActor> {

        ServiceActor service;
        volatile int success = 0;

        public void init(ServiceActor service) {
            this.service = service;
        }


        public void callbackTest() {

            final Thread callerThread = Thread.currentThread();
            service.getString(inThread(self(),new SomeCallbackHandler() {
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
//
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

        assertTrue(cbActor.getActor().success == 2);
        cbActor.$stop();
        service.$stop();

    }

    public static class Overload extends Actor {

        public void a(int x, Callback<Integer> cb) {
            cb.receive(x, null);
        }

        public void a(int x, int y, Callback<Integer> cb) {
            cb.receive(y, null);
        }

    }


    @Test
    public void testOverload() {
        try {
            // verify exception is thrown
            Overload ov = AsActor(Overload.class);
            assertTrue(false);
        } catch (Exception e) {
            // expected, cannot overload with argument types, number (too expensive)
            //e.printStackTrace();
        }
    }


    public static class SleepActor extends Actor<SleepActor> {

        private String name;

        public Future init(String na) {
            name = na;
            return new Promise(na);
        }

        public Future<String> getName() {
            return new Promise<>(name);
        }

        public Future $sleep(long millis) {
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new Promise<>("void");
        }

        public Future<Long> sleep() {
            long millis = (long) (Math.random() * 1000);
            try {
                Thread.sleep(millis);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return new Promise<>(millis);
        }

        public Future<String> say( String s ) {
            System.out.println(name+" says '"+s+"'");
            return new Promise<>("result "+s);
        }

    }

    public static class SleepCallerActor extends Actor<SleepCallerActor> {
        SleepActor act[];
        Future<Long> results[];

        public void test() {
            act = new SleepActor[10];
            results = new Future[act.length];
            for (int i = 0; i < act.length; i++) {
                act[i] = Actors.AsActor(SleepActor.class);
                act[i].init("("+i+")");
            }

            for (int i = 0; i < act.length; i++) {
                results[i] = act[i].sleep();
            }

            yield(results).then(new Callback<Future[]>() {
                @Override
                public void receive(Future[] result, Object error) {
                    System.out.println("now "+System.currentTimeMillis());
                    for (int i = 0; i < result.length; i++) {
                        Future future = result[i];
                        System.out.println("sleep "+i+" "+future.getResult());
                    }
                }
            });

        }

        public void $stop() {
            for (int i = 0; i < act.length; i++) {
                act[i].$stop();
            }
             super.$stop();
        }


    }


    @Test @Ignore
    public void testYield() {

        SleepCallerActor act = Actors.AsActor(SleepCallerActor.class);
        System.out.println("now "+System.currentTimeMillis());
        act.test();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        act.$stop();
    }

    public static class TestBlockingAPI extends Actor<TestBlockingAPI> {

        public Future<String> get( final String url ) {
            final Promise<String> content = new Promise();
            final Thread myThread = Thread.currentThread();
            async(new Callable<String>() {
                      @Override
                      public String call() throws Exception {
                          return new Scanner(new URL(url).openStream(), "UTF-8").useDelimiter("\\A").next();
                      }
                  }
            ).then(
                    new Callback<String>() {
                        @Override
                        public void receive(String result, Object error) {
                            if (Thread.currentThread() == myThread) {
                                content.receive(result, null);
                            } else {
                                content.receive(null, "wrong thread");
                            }
                        }
                    });
            return content;
        }
    }

    public static class FutureTest extends Actor<FutureTest> {

        public Future<String> getString( String s ) {
            return new Promise<>(s+"_String");
        }

    }

    public static class FutureTestCaller extends Actor<FutureTestCaller> {

        FutureTest ft;

        public void init() {
            ft = Actors.AsActor(FutureTest.class);
        }

        public Future<String> doTestCall() {
            final Promise<String> stringResult = new Promise<String>().setId("doTestCall");
            ft.getString("13")
                .then(new Callback<String>() {
                    @Override
                    public void receive(String result, Object error) {
                        stringResult.receive(result, null);
                    }
                });
            return stringResult;
        }

        public void doTestCall1(final Future<String> stringResult) {
            ft.getString("13")
                    .then(new Callback<String>() {
                        @Override
                        public void receive(String result, Object error) {
                            stringResult.receive(result, null);
                        }
                    });
        }
    }

    @Test
    public void testFuture() {
        FutureTest ft = Actors.AsActor(FutureTest.class);
        final AtomicReference<String> outerresult0 = new AtomicReference<>();
        ft.getString("oj").then(new Callback<String>() {
            @Override
            public void receive(String result, Object error) {
                System.out.println("simple:" + result);
                outerresult0.set(result);
            }
        });

        FutureTestCaller test = Actors.AsActor(FutureTestCaller.class);
        test.init();

        final AtomicReference<String> outerresult = new AtomicReference<>();
        test.doTestCall()
            .then(new Callback<String>() {
                @Override
                public void receive(String result, Object error) {
                    System.out.println("outer result " + result);
                    outerresult.set(result);
                }
            });

        final AtomicReference<String> outerresult1 = new AtomicReference<>();
        Future<String> f = new Promise<>();
        test.doTestCall1(f);
        f.then(new Callback<String>() {
            @Override
            public void receive(String result, Object error) {
                System.out.println("outer1 result:"+result);
                outerresult1.set(result);
            }
        });

        try {
            while( outerresult1.get() == null )
                Thread.sleep(100);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        assertTrue(outerresult0.get().equals("oj_String"));
        assertTrue(outerresult.get().equals("13_String"));
        assertTrue(outerresult1.get().equals("13_String"));
    }


    public static class DelayedTest extends Actor<DelayedTest> {

        public void $delay(long started) {
            delay_threads.set(__currentDispatcher == Thread.currentThread());
            if (!delay_threads.get()) {
                System.out.println("current thread " + Thread.currentThread().getName());
                System.out.println("dispatcher " + __currentDispatcher.getName());
            }
            System.out.println("ThreadsCheck:" + delay_threads.get());
            long l = System.currentTimeMillis() - started;
            System.out.println("DELAY:" + l);
            delay_time.set(l);
        }
    }

    final static AtomicBoolean delay_threads = new AtomicBoolean(false);
    final static AtomicLong delay_time = new AtomicLong(0);
    final static AtomicLong delay_err = new AtomicLong(0);

    public static class DelayedCaller extends Actor {

        public void $dummy() {
            System.out.println("pok");
        }

        public void $delay() {
            final DelayedTest test = Actors.AsActor(DelayedTest.class);
            final long now = System.currentTimeMillis();
            delayed(100, () -> {
                if ( Thread.currentThread() != __currentDispatcher )
                    delay_err.incrementAndGet();
                test.$delay(now);
                test.$stop();
            });
        }
    }

    @Test @Ignore
    public void testDelayed() {
        DelayedCaller caller = Actors.AsActor(DelayedCaller.class);
        caller.$delay();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        assertTrue(delay_threads.get());
        assertTrue(delay_err.get()==0);
        assertTrue(delay_time.get() >= 100 && delay_time.get() < 200);
        caller.$stop();
    }

    @Test
    public void testBlockingCall() {
        final AtomicInteger success = new AtomicInteger(0);
        TestBlockingAPI actor = AsActor(TestBlockingAPI.class);
        actor.get("http://www.google.com" ).then( new Callback<String>() {
            @Override
            public void receive(String result, Object error) {
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
        actor.$stop();
    }


}
