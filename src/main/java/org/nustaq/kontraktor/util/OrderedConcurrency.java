package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 07.09.14.
 */
public class OrderedConcurrency {

    public static final int ENTRIES_PER_THREAD = 128;

    static class OCJob {
        volatile Object result = NO_RES;
        volatile Callable toCall;
    }

    OCJob jobs[];
    int addIndex;
    int remIndex;

    public OrderedConcurrency(int threads) {
        threads*=ENTRIES_PER_THREAD;
        if ( Integer.bitCount(threads) != 1  )
            throw new RuntimeException("numthreads must be a power of 2");
        jobs = new OCJob[threads];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new OCJob();
            final int finalI = i;
            if ( (i % ENTRIES_PER_THREAD) == ENTRIES_PER_THREAD-1 ) {
                Thread t = new Thread(() -> work(finalI/ENTRIES_PER_THREAD), "worker " + i);
                t.start();
            }
        }
    }

    /**
     * single threaded
     * @param todo
     * @return true if adding was successful
     */
    public boolean offer( Callable todo ) {
        if ( jobs[addIndex].toCall != null ) {
            return false;
        }
        jobs[addIndex].toCall = todo;
        jobs[addIndex].result = NO_RES;
        addIndex = (addIndex +1) & (jobs.length-1);
        return true;
    }

    final static Object NO_RES = "--NLL___";
    /**
     * must be same thread as add
     * @return NO_RES if no result
     */
    public Object poll() {
        if ( jobs[remIndex].result != NO_RES && jobs[remIndex].toCall != null ) {
            Object res = jobs[remIndex].result;
            jobs[remIndex].toCall = null;
            jobs[remIndex].result = NO_RES;
            remIndex = (remIndex +1) & (jobs.length-1);
            return res;
        }
        return NO_RES;
    }

    volatile boolean stopped = false;

    public void stop() {
        stopped = true;
    }

    public void work( int index ) {
        int base = index*ENTRIES_PER_THREAD;
        int count = 0;
        while( ! stopped ) {
            OCJob job = jobs[base+count];
            while (job.result != NO_RES) {
                // spin
            }
            while (job.toCall == null) {
                // spin
            }
            Object res = null;
            try {
                res = job.toCall.call();
            } catch (Exception e) {
                res = e;
            }
            job.result = res;
            count++;
            if ( count == ENTRIES_PER_THREAD ) {
                count = 0;
            }
        }
    }

    public static class DecAct extends Actor<DecAct> {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

        public Future decodeOrdered( Future previous, Object o ) {
            Promise<Object> promise = new Promise<>();
            byte[] result = conf.asByteArray((Serializable) o);
            if ( previous == null ) {
                promise.receive(result,null);
            } else {
                previous.then((res, err) -> promise.receive(result, null));
            }
            return promise;
        }
    }


    public static void main(String arg[]) throws InterruptedException {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        ArrayList toEncode = new ArrayList();
        for ( int i = 0; i < 1000000; i++ ) {
            HashMap e = new HashMap();
            e.put("VALUE", new Object[] { i, 11111111,22222222,33333333, "pok"} );
            e.put("XY", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
            e.put("XY1", new Object[] { "aposdj", "POK", 42223222,333333, "poasdasdasdk"} );
//            e.put("XY2", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
//            e.put("XY3", new Object[] { "aposdj", "POK", 42223222,333333, "poasdasdasdk"} );
//            e.put("XY4", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
//            e.put("XY5", new Object[] { "aposdj", "POK", 42223222,333333, "poasdasdasdk"} );
            toEncode.add(e);
        }

        ThreadLocal<FSTConfiguration> cfg = new ThreadLocal() {
            @Override
            protected Object initialValue() {
                return FSTConfiguration.createDefaultConfiguration();
            }
        };

        int parcount = 0;
        AtomicInteger count = new AtomicInteger(0);
        ParallelizingActor parAct = Actors.AsActor(ParallelizingActor.class,30000);
        parAct.$init(4, (res,err) -> {
            count.incrementAndGet();
        });
        while( parcount < -10 ) {
            count.set(0);
            long tim = System.currentTimeMillis();
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                parAct.$execute( () -> {
                    return cfg.get().asByteArray(toEnc);
                });
            }
            while( count.get() < 999000 ) { // fimxe: its bugged!
//                parAct.$repoll();
            }
            System.out.println("time ParACT: " + (System.currentTimeMillis() - tim)+" num:"+count.get());
            parcount++;
        }
        parAct.$stop();


        int actcount = 0;
        DecAct acts[] = new DecAct[4];
        for (int i = 0; i < acts.length; i++) {
            acts[i] = Actors.AsActor(DecAct.class,16000);
        }
        while( actcount < 10) {

            long tim = System.currentTimeMillis();
            int actIdx = 0;
            Future last = null;
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                last = acts[actIdx].decodeOrdered(last, toEnc).then((res, err) -> {
                    // result handling
//                    HashMap o = (HashMap) FSTConfiguration.getDefaultConfiguration().asObject((byte[]) res);
//                    System.out.println("res: "+ ((Object[]) o.get("VALUE"))[0]);
                });
                actIdx++;
                if ( actIdx >= acts.length )
                    actIdx = 0;
            }
            CountDownLatch latch = new CountDownLatch(acts.length);
            for (int i = 0; i < acts.length; i++) {
                DecAct act = acts[i];
                act.$sync().then((r, e) -> {
                    latch.countDown();
                });
            }
            latch.await();
            System.out.println("time ACT: " + (System.currentTimeMillis() - tim));
            actcount++;
        }
        for (int i = 0; i < acts.length; i++) {
            acts[i].$stop();
        }

        int stcount = 0;
        while( stcount < 10) {

            long tim = System.currentTimeMillis();
            int resCount = 0;
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                byte b[] = conf.asByteArray(toEnc);
                if ( b[0] == 255 )
                    System.out.println("POK");
            }
            System.out.println("time ST: "+(System.currentTimeMillis()-tim));
            stcount++;

        }

        OrderedConcurrency conc = new OrderedConcurrency(4);
        while( true ) {

            long tim = System.currentTimeMillis();
            int resCount = 0;
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                Callable todo = () -> {
                    return cfg.get().asByteArray(toEnc);
                };
                while (! conc.offer(todo) ) {
                    Object res = null;
                    do {
                        res = conc.poll();
                        if ( res != NO_RES ) {
                            if ( res instanceof Exception ) {
                                ((Exception) res).printStackTrace();
                                return;
                            }
                            else {
//                                HashMap o = (HashMap) conf.asObject((byte[]) res);
//                                System.out.println("res: "+ ((Object[]) o.get("VALUE"))[0]);
                            }
                            resCount++;
                        }
                    } while( res != NO_RES );
                }
            }
            System.out.println("time MT: "+(System.currentTimeMillis()-tim+" rescount:"+resCount));
        }
    }
}
