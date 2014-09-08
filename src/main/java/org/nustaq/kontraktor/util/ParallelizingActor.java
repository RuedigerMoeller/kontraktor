package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Future;

import java.util.concurrent.Callable;

/**
 * Created by ruedi on 08.09.14.
 */
public class ParallelizingActor extends Actor<ParallelizingActor> {

    OrderedConcurrency conc = null;
    Callback resultStream;
    int jobsOpen = 0;

    public void $init( int threads, Callback resultStream ) {
        conc = new OrderedConcurrency(threads);
        this.resultStream = resultStream;
    }

    public void $execute( Callable toCall ) {
        jobsOpen++;
        while ( ! conc.offer(toCall) ) {
            $repoll();
        }
    }

    public void $repoll() {
        Object poll;
        while ( (poll = conc.poll()) != conc.NO_RES ) {
            resultStream.receive(poll,null);
            jobsOpen--;
        }
    }

    @Override
    public void $stop() {
        conc.stop();
        super.$stop();
    }
}
