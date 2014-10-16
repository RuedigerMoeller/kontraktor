package kontraktor.scheduling.exectest;

import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * Created by ruedi on 10/13/14.
 */
public class Work2 {

    public Random rand = new Random(13);

    int localState[];

    public Work2(int localSize) {
        this.localState = new int[localSize];
    }

    public int doWork(int iterations, Executor executor, int countDown, CountDownLatch latch ) {
        int sum = 0;
        for ( int i = 0; i < iterations; i++ ) {
            int index = rand.nextInt(localState.length);
            sum += localState[index];
            localState[index] = i;
        }
        if ( countDown > 0 ) {
            // submit next message
            executor.execute( () -> doWork(iterations,executor,countDown-1,latch) );
        } else {
            // finished
            latch.countDown();
        }
        return sum;
    }

}