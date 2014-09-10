package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 07.09.14.
 */
public class Hoarde<T extends Actor> {

    Actor actors[];
    int index = 0;
    Promise prev;

    public Hoarde(int numThreads, Class<T> actor) {
        actors = new Actor[numThreads];
        for (int i = 0; i < actors.length; i++) {
            actors[i] = Actors.AsActor(actor);
        }
    }

    public Future[] each(BiFunction<T, Integer, Future> init) {
        Future res[] = new Future[actors.length];
        for (int i = 0; i < actors.length; i++) {
            T actor = (T) actors[i];
            res[i] = init.apply(actor,i);
        }
        return res;
    }

    public void each(Consumer<T> init) {
        for (int i = 0; i < actors.length; i++) {
            init.accept( (T) actors[i] );
        }
    }

    public Future ordered(Function<T, Future> toCall) {
        final Future result = toCall.apply((T) actors[index]);
        index++;
        if (index==actors.length)
            index = 0;
        if ( prev == null ) {
            prev = new Promise();
            result.then(prev);
            return prev;
        } else {
            Promise p = new Promise();
            prev.getNext().finishWith( (res, err) -> result.then( (res1,err1) -> p.receive(res1, err1) ) );
            prev = p;
            return p;
        }
    }


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

        // warmup
        testSingleThreaded(conf, toEncode, 1);
        testGenericAct(toEncode, 1);
//        testManualActor(toEncode, 1);


        testSingleThreaded(conf, toEncode, 10);
        testGenericAct(toEncode, 10);
//        testManualActor(toEncode, 10);


    }

    private static void testGenericAct(ArrayList toEncode, int max) throws InterruptedException {
        int actcount = 0;
        Hoarde<DecAct> oc = new Hoarde<DecAct>(4,DecAct.class);
        oc.each( (dec) -> dec.$init() );
        AtomicInteger lastVal = new AtomicInteger(-1);
        while( actcount < max) {

            long tim = System.currentTimeMillis();
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                oc.ordered((act) -> {
                    return act.decode(toEnc);
                }).then((r, e) -> {
//                    HashMap o = (HashMap) FSTConfiguration.getDefaultConfiguration().asObject((byte[]) r);
//                    Integer value = (Integer) ((Object[]) o.get("VALUE"))[0];
//                    if ( lastVal.get() != value.intValue()-1)
//                        System.out.println("ERROR "+value+" "+lastVal);
//                    lastVal.set(value.intValue());
//                    System.out.println("res: "+ value);
                });
            }
            CountDownLatch latch = new CountDownLatch(4);
            oc.each((act, i) -> act.$sync().then((r, e) -> latch.countDown()));
            latch.await();
            System.out.println("time GEN_ACT: " + (System.currentTimeMillis() - tim));
            actcount++;
        }
        oc.each( (act) -> act.$stop());
    }

    private static void testSingleThreaded(FSTConfiguration conf, ArrayList toEncode, int max) {
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
            System.out.println("time ST: "+(System.currentTimeMillis()-tim));
            stcount++;

        }
    }

    private static void testManualActor(ArrayList toEncode, int max) throws InterruptedException {
        int actcount = 0;
        DecAct acts[] = new DecAct[4];
        for (int i = 0; i < acts.length; i++) {
            acts[i] = Actors.AsActor(DecAct.class, 320000);
        }
        while( actcount < max) {

            long tim = System.currentTimeMillis();
            int actIdx = 0;
            Future last = null;
            for (int i = 0; i < toEncode.size(); i++) {
                HashMap<Object, Object> toEnc = (HashMap<Object, Object>) toEncode.get(i);
                last = acts[actIdx].decodeOrdered(last, toEnc).then( (res, err) -> {
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
    }
}
