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

/**
 * Created by ruedi on 07.09.14.
 */
public class OrderedConcurrency {

    static class OCJob {
        volatile Object result = NO_RES;
        volatile Callable toCall;
    }

    OCJob jobs[];
    int addIndex;
    int remIndex;

    public OrderedConcurrency(int threads) {
        threads*=16;
        if ( Integer.bitCount(threads) != 1  )
            throw new RuntimeException("numthreads must be a power of 2");
        jobs = new OCJob[threads];
        for (int i = 0; i < jobs.length; i++) {
            jobs[i] = new OCJob();
            final int finalI = i;
            if ( (i % 16) == 15 ) {
                Thread t = new Thread(() -> work(finalI/16), "worker " + i);
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
     * @return NULL if no result
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

    public void work( int index ) {
        int base = index*16;
        int count = 0;
        while( true ) {
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
            if ( count == 16 ) {
                count = 0;
            }
        }
    }

    public static class DecAct extends Actor<DecAct> {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        public Future decode( Object o ) {
            return new Promise<>(conf.asByteArray((Serializable) o));
        }
    }


    public static void main(String arg[]) throws InterruptedException {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        ArrayList toEncode = new ArrayList();
        for ( int i = 0; i < 1000000; i++ ) {
            HashMap e = new HashMap();
            e.put("VALUE", new Object[] { i, 11111111,22222222,33333333, "pok"} );
            e.put("XY", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
            toEncode.add(e);
        }

        int actcount = 0;
        DecAct act = Actors.AsActor(DecAct.class,5000);
        while( actcount < 10) {

            long tim = System.currentTimeMillis();
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                act.decode(toEnc);
            }
            CountDownLatch latch = new CountDownLatch(1);
            act.$sync().then((r, e) -> {
                System.out.println("time ACT: " + (System.currentTimeMillis() - tim));
                latch.countDown();
            });
            latch.await();
            actcount++;
        }
        act.$stop();

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
        ThreadLocal<FSTConfiguration> cfg = new ThreadLocal() {
            @Override
            protected Object initialValue() {
                return FSTConfiguration.createDefaultConfiguration();
            }
        };
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
            System.out.println("time MT: "+(System.currentTimeMillis()-tim));

        }
    }
}
