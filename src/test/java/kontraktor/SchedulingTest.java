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

        public void $generateLoad(int count) {
            if ( threadIn ) {
                System.out.println("fatal");
                System.exit(0);
            }
            threadIn = true;
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

    public static class SchedAct extends Actor<SchedAct> {

        HoardeAct test[];
        int count = 0;

        public void $init() {

            test = new HoardeAct[16];
            for ( int i = 0; i < test.length; i++ ) {
                test[i] = Actors.AsActor(HoardeAct.class);
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
            test[count].$generateLoad(2000*(count+1));
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
            yield(results).then( new Callback<Future[]>() {
                @Override
                public void receiveResult(Future[] results, Object error) {
                    boolean hadDiff = false;
                    long prev = (long) results[0].getResult();
                    for (int i = 0; i < results.length; i++) {
                        long r = (long) results[i].getResult();
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
                    done.receiveResult("void",null);
                }
            });
            return done;
        }
    }

    static ElasticScheduler scheduler = new ElasticScheduler(8, 30000);
    public static void main(String a[]) throws InterruptedException {
        while( true ) {
            final SchedAct act = Actors.AsActor(SchedAct.class, scheduler);
            act.$init();
            long tim = System.currentTimeMillis();
            long count = 0;
            int speed = 1;
            while (true) {
                act.$tick();
                if ((count % speed) == 0) {
                    LockSupport.parkNanos(10);
                }
                count++;
                long diff = System.currentTimeMillis() - tim;
                if (diff > 1000) {
                    System.out.println("Count:" + count * 1000 / diff + " " + diff + " spd " + speed);
                    count = 0;
                    tim = System.currentTimeMillis();
                    speed++;
                    speed++;
//                    speed++;
//                    speed++;
                    if (speed > 200)
                        break;
                }
            }
            Thread.sleep(2000);
            act.$dumpCalls().then(new Callback() {
                @Override
                public void receiveResult(Object result, Object error) {
                    act.$stop();
                        new Thread() { public void run() {
                            try {
                                main(null);
                            } catch (InterruptedException e) {
                                e.printStackTrace();
                            }
                        }}.start();
                }
            });
        }
    }
}
