package kontraktor;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 4/9/2016.
 */
public class BufferingTest {

    public static class TBuffActor extends Actor<TBuffActor> {
        List<Integer> counter = new ArrayList<>();

        public IPromise promise(int count) {
            counter.add(count);
            return resolve(count);
        }

        public void cbTest(int count, Callback cb) {
            cb.pipe(count);
            cb.finish();
        }
    }

//    @Test
//    public void testBuf() throws InterruptedException {
//        TBuffActor tBuffActor = Actors.AsBufferedActor(TBuffActor.class);
//        TBuffActor act = Actors.AsActor(TBuffActor.class);
//
//        AtomicInteger err = new AtomicInteger();
//        AtomicInteger res = new AtomicInteger();
//
//        IntStream.range(0,1000).forEach( i -> tBuffActor.promise(i).then( r -> {
//            if ( ! r.equals(i) )
//                err.incrementAndGet();
//            else
//                res.incrementAndGet();
//        }));
//        IntStream.range(0,1000).forEach( i -> tBuffActor.cbTest(i, (r,e) -> {
//            if ( r != null && ! r.equals(i) )
//                err.incrementAndGet();
//            if ( r != null && r.equals(i) )
//                res.incrementAndGet();
//            if ( r == null && e == null )
//                res.incrementAndGet();
//        }));
//
//        Thread.sleep(2000);
//
//        Assert.assertTrue( res.get() == 0 && err.get() == 0 );
//        tBuffActor.transferTo(act);
//
//        Thread.sleep(2000);
//        System.out.println("res "+res+" "+err);
//        Assert.assertTrue( res.get() == 3000 );
//    }

}
