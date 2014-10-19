package kontraktor.scheduling.exectest;

import java.awt.*;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Created by ruedi on 10/13/14.
 */
public class Work2 {

//    public Random rand = new Random(13);
//
//    int localState[];
//
//    public Work2(int localSize) {
//        this.localState = new int[localSize];
//    }
//
//    public int doWork(int iterations, Executor executor, int countDown, CountDownLatch latch ) {
//        int sum = 0;
//        for ( int i = 0; i < iterations; i++ ) {
//            int index = rand.nextInt(localState.length);
//            sum += localState[index];
//            localState[index] = i;
//        }
//        if ( countDown > 0 ) {
//            // submit next message
//            executor.execute( () -> doWork(iterations,executor,countDown-1,latch) );
//        } else {
//            // finished
//            latch.countDown();
//        }
//        return sum;
//    }

    // more realistic testcase below (comment out everything above then)

    public Random rand = new Random(13);

    HashMap localState;
    int hmapSize;
    public Work2(int localSize) {
        this.localState = new HashMap();
        this.hmapSize = localSize;
    }

    public int doWork(int iterations, Executor executor, int countDown, CountDownLatch latch ) {

        int key = rand.nextInt(hmapSize);
        if ( localState.get(key) == null ) { // redundant hash accesses by intent
            localState.put(""+key, new Point(countDown, countDown));
        } else {
            Point p = (Point) localState.get(""+key);
            key = p.y;
        }

        if ( countDown > 0 ) {
            // submit next message
            executor.execute( () -> doWork(iterations,executor,countDown-1,latch) );
        } else {
            // finished
            latch.countDown();
        }
        return key;
    }

}