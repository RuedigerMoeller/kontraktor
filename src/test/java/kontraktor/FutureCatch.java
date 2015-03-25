package kontraktor;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.nustaq.kontraktor.Actors.*;

/**
 * Created by ruedi on 25.03.2015.
 */
public class FutureCatch {

    public static class FutCatch extends Actor<FutCatch> {

        public Future error(int num) {
            Promise res = new Promise();
            delayed( 500, () -> res.settle(null, "Error " + num) );
            return res;
        }

        public Future result(int num) {
            Promise res = new Promise();
            delayed( 500, () -> res.settle("Result " + num, null) );
            return res;
        }

    }

    @Test
    public void testFut() throws InterruptedException {
        AtomicBoolean res = new AtomicBoolean(true);
        final FutCatch futCatch = AsActor(FutCatch.class);
        AtomicInteger count = new AtomicInteger(0);
        Actors.sync(
            futCatch.result(0)
               .then(r -> {
                   System.out.println("" + r);
                   count.addAndGet(1);
                   return futCatch.result(1);
               })
               .then(r -> {
                   System.out.println("" + r);
                   count.addAndGet(2);
                   return futCatch.result(2);
               })
               .then(r -> {
                   System.out.println("" + r);
                   count.addAndGet(3);
                   return futCatch.error(1);
               })
               .then(r -> {
                   System.out.println("" + r);
                   count.addAndGet(4);
                   return futCatch.result(3);
               })
               .then(r -> {
                   System.out.println("" + r);
                   count.addAndGet(5);
                   return futCatch.result(4);
               })
               .catchError(error -> {
                   count.addAndGet(5);
                   System.out.println("catched " + error);
               })
               .then((r, e) -> System.out.println("done "+count.get()))
        );
        Assert.assertTrue(count.get() == 11);
    }

}
