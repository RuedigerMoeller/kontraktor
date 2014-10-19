package kontraktor.scheduling.exectest;

import org.nustaq.kontraktor.Actor;

import java.awt.*;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Created by ruedi on 16.10.2014.
 */
public class WorkActor extends Actor<WorkActor> {
//    public Random rand = new Random(13);
//
//    int localState[];
//
//    public void $init(int localSize) {
//        this.localState = new int[localSize];
//    }
//
//    public void $doWork(int iterations ) {
//        int sum = 0;
//        for ( int i = 0; i < iterations; i++ ) {
//            int index = rand.nextInt(localState.length);
//            sum += localState[index];
//            localState[index] = i;
//        }
//        return sum;
//    }

// more realistic testcase below (comment out everything above then)

    public Random rand = new Random(13);

    HashMap localState;
    int hmapSize;
    int lastRes;
    public void $init(int localSize) {
        this.localState = new HashMap();
        this.hmapSize = localSize;
    }

    public void $doWork(int iterations) {

        int key = rand.nextInt(hmapSize);
        if ( localState.get(key) == null ) { // redundant hash accesses by intent
            localState.put(""+key, new Point(iterations, iterations));
        } else {
            Point p = (Point) localState.get(""+key);
            key = p.y;
        }

        lastRes = key;
    }

}
