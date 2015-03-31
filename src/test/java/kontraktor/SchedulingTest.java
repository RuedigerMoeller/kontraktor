package kontraktor;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.ElasticScheduler;

import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 10.06.14.
 */
public class SchedulingTest {


    public static class HoardeAct extends Actor<SchedAct> {

        volatile boolean threadIn = false;
        long sim[] = new long[64];
        long callsReceived = 0;
        long lastSequence = 0;

        public void $generateLoad(int count, long sequence) {
            if ( threadIn ) {
                System.out.println("fatal");
                System.exit(0);
            }
            threadIn = true;
            if ( lastSequence != 0 && lastSequence != sequence-32 ) {
                System.out.println("fatal sequence error");
                System.exit(0);
            }
            lastSequence = sequence;
            long sum = 0;
            long max = count;
            for (int i = 0; i < max; i++ ) {
                sum += i;
                sim[i&63] = (int) sum;
            }
            callsReceived++;
            threadIn = false;
        }

        public void $dumpCalls(int i) {
            System.out.println("" + i + " - " + callsReceived);
        }

        public Future<Integer> $getCalls() {
            return new Promise(callsReceived);
        }

    }

    static boolean THREADCOMPARE = false;
    static boolean OPTIMAL = true;
    static boolean THREADPERACT = false;
    public static class SchedAct extends Actor<SchedAct> {

        HoardeAct test[];
        int count = 0;
        long sequence = 0;

        public void $init() {

            test = new HoardeAct[32];
            ElasticScheduler scheds[] = new ElasticScheduler[8];
            if ( THREADCOMPARE && ! THREADPERACT) {
                for (int i = 0; i < scheds.length; i++) {
                    scheds[i] = new ElasticScheduler(1, DEF_Q_SIZE);
                }
            }
            for ( int i = 0; i < test.length; i++ ) {
                if ( THREADCOMPARE ) {
                    if ( THREADPERACT ) {
                        test[i] = Actors.AsActor(HoardeAct.class, new ElasticScheduler(1, DEF_Q_SIZE) );
                    } else
                    if ( OPTIMAL ) {
                        if ((i & 1) == 0)
                            test[i] = Actors.AsActor(HoardeAct.class, scheds[i / 4]);
                        if ((i & 1) == 1)
                            test[i] = Actors.AsActor(HoardeAct.class, scheds[7 - i / 4]);
                    } else {
                        test[i] = Actors.AsActor(HoardeAct.class, scheds[i / 4]);
                    }
                } else
                    test[i] = Actors.AsActor(HoardeAct.class,scheduler);
            }

        }

        public void $stop() {
            for (int i = 0; i < test.length; i++) {
                HoardeAct hoardeAct = test[i];
                hoardeAct.$stop();
            }
            super.$stop();
        }

        public void $tick() {
            test[count].$generateLoad(2000*(count+1), sequence++);
            count++;
            if ( count >= test.length )
                count = 0;
        }


        public Future $dumpCalls() {
            final Promise done = new Promise();
            Future<Integer> results[] = new Future[test.length];
            for (int i = 0; i < test.length; i++) {
                results[i] = test[i].$getCalls();
            }
            all(results).then( (res, error ) -> {
                boolean hadDiff = false;
                long prev = (long) res[0].get();
                for (int i = 0; i < res.length; i++) {
                    long r = (long) res[i].get();
                    System.out.println("" + i + " - " + r);
                    if ( r != prev ) {
                        if (hadDiff) {
                            System.out.println("fatal error 1");
                            System.exit(0);
                        }
                        if ( r == prev-1 ) {
                            prev = r;
                            hadDiff = true;
                        } else {
                            System.out.println("fatal error 0 "+r);
                            System.exit(0);
                        }
                    }
                }
                done.complete("void", null);
            });
            return done;
        }
    }

    public static final int DEF_Q_SIZE = 10000;
    static ElasticScheduler scheduler = new ElasticScheduler(8, DEF_Q_SIZE);
    public static void main(String a[]) throws InterruptedException {
        final SchedAct act = Actors.AsActor(SchedAct.class, new ElasticScheduler(1, DEF_Q_SIZE));
        act.$init();
        long tim = System.currentTimeMillis();
        long count = 0;
        int speed = 1;
        int direction = 2;
        while (true) {
            act.$tick();
            if ((count % (Math.max(1,speed))) == 0) {
                LockSupport.parkNanos(100000);
            }
            count++;
            long diff = System.currentTimeMillis() - tim;
            if (diff > 1000) {
                System.out.println("Count:" + count * 1000 / diff + " " + diff + " spd " + speed);
                count = 0;
                tim = System.currentTimeMillis();
                speed+=direction;
                if (speed > 200) {
                    if ( direction > 0 )
                        direction = -2 * direction;
                }
                if (speed < -100) {
                    break;
                }
            }
        }
        while( scheduler.getActiveThreads() > 1 ) {
            Thread.sleep(1000);
            System.out.println("waiting for end "+scheduler.getActiveThreads());
        }
        act.$dumpCalls().then(new Callback() {
            @Override
            public void complete(Object result, Object error) {
                act.$stop();
                    new Thread() { public void run() {
                        try {
                            while( scheduler.getActiveThreads() > 0 ) {
                                Thread.sleep(1000);
                                System.out.println("waiting "+scheduler.getActiveThreads());
                            }
                            System.out.println("finished waiting .. restart in 5 sec");
                            Thread.sleep(5000);
                            System.out.println(".. restart");
                            main(null);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }}.start();
            }
        });
    }
}
