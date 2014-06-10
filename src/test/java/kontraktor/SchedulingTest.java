package kontraktor;

import de.ruedigermoeller.kontraktor.Actor;
import de.ruedigermoeller.kontraktor.Actors;

import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 10.06.14.
 */
public class SchedulingTest {

    public static class HoardeAct extends Actor<SchedAct> {

        long sim[] = new long[64];

        public void $generateLoad(int count) {
            long sum = 0;
            long max = count;
            for (int i = 0; i < max; i++ ) {
                sum += i;
                sim[i&63] = (int) sum;
            }
        }
    }

    public static class SchedAct extends Actor<SchedAct> {

        HoardeAct test[];

        public void $init() {

            test = new HoardeAct[10];
            for ( int i = 0; i < test.length; i++ ) {
                test[i] = Actors.AsActor(HoardeAct.class);
            }

        }

        public void $tick() {
            for (int i = 0; i < test.length; i++) {
                HoardeAct hoardeAct = test[i];
                hoardeAct.$generateLoad(i*2000);
            }
        }

    }

    public static void main(String a[]) {
        SchedAct act = Actors.AsActor(SchedAct.class);
        act.$init();
        long tim = System.currentTimeMillis();
        long count = 0;
        int speed = 1;
        while( true ) {
            act.$tick();
            if ( (count%speed) == 0 ) {
                LockSupport.parkNanos(1);
            }
            count++;
            long diff = System.currentTimeMillis() - tim;
            if ( diff > 1000 ) {
                System.out.println("Count:" + count * 1000 / diff + " " + diff + " spd " + speed);
                count = 0;
                tim = System.currentTimeMillis();
            }
        }
    }
}
