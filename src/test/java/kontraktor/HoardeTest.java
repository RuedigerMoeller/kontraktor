package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Hoarde;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 11.09.14.
 */
public class HoardeTest {

    /////////////////////////////////////////////////// test/benchmark ///////////////////////////////////


    public static class DecAct extends Actor<DecAct> {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();

        public void $init() {

        }

        public Future decode(Object o) {
            return new Promise<>(conf.asByteArray((Serializable) o));
        }

        public Future decodeOrdered(Future previous, Object o) {
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



    @Test
    public void run() throws InterruptedException {
        FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
        ArrayList toEncode = new ArrayList();
        for ( int i = 0; i < 1000000; i++ ) {
            HashMap e = new HashMap();
            e.put("VALUE", new Object[] { i, 11111111,22222222,33333333, "pok"} );
            e.put("XY", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
            e.put("XY1", new Object[] { "aposdj", "POK", 42223222,333333, "poasdasdasdk"} );
            e.put("XY2", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
            e.put("XY3", new Object[] { "aposdj", "POK", 42223222,333333, "poasdasdasdk"} );
            e.put("XY4", new Object[] { "aposdj", "POK", 422222222,333323333, "poasdasdk"} );
            e.put("XY5", new Object[] { "aposdj", "POK", 42223222,333333, "poasdasdasdk"} );
            toEncode.add(e);
        }

        // warmup
        testSingleThreaded(conf, toEncode, 1);
        testGenericAct(toEncode, 2, 1, true );


        testSingleThreaded(conf, toEncode, 6);
        testGenericAct(toEncode, 4, 5, false );

        System.out.println("DONE");
    }

    private void testGenericAct(ArrayList toEncode, int threads, int max, boolean verify) throws InterruptedException {
        int actcount = 0;
        Hoarde<DecAct> oc = new Hoarde<DecAct>(threads,DecAct.class);
        oc.each( (dec) -> dec.$init() );
        AtomicInteger count = new AtomicInteger(0);
        AtomicInteger lastVal = new AtomicInteger(-1);
        while( actcount < max) {

            long tim = System.currentTimeMillis();
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                oc.ordered((act) -> {
                    return act.decode(toEnc);
                }).then((r, e) -> {
                    count.incrementAndGet();
                    if ( verify ) {
                        HashMap o = (HashMap) FSTConfiguration.getDefaultConfiguration().asObject((byte[]) r);
                        Integer value = (Integer) ((Object[]) o.get("VALUE"))[0];
                        if (lastVal.get() != value.intValue() - 1)
                            System.out.println("ERROR " + value + " " + lastVal);
                        lastVal.set(value.intValue());
                    }
                });
            }
            while( count.get() < 1000000 ) {
                LockSupport.parkNanos(1000*1000);
            }
            System.out.println("time Actor: " + (System.currentTimeMillis() - tim) + " " + count.get());
            count.set(0);
            actcount++;
        }
        oc.each( (act) -> act.$stop());
    }

    private void testSingleThreaded(FSTConfiguration conf, ArrayList toEncode, int max) {
        int stcount = 0;
        while( stcount < max) {

            long tim = System.currentTimeMillis();
            int resCount = 0;
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                byte b[] = conf.asByteArray(toEnc);
                if ( b[0] == 255 )
                    System.out.println("POK");
            }
            System.out.println("time Singe Threaded: "+(System.currentTimeMillis()-tim));
            stcount++;

        }
    }

}
