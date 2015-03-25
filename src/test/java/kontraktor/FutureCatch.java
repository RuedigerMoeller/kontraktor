package kontraktor;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Future;
import org.nustaq.kontraktor.Promise;

import java.util.concurrent.atomic.AtomicBoolean;

import static org.nustaq.kontraktor.Actors.*;

/**
 * Created by ruedi on 25.03.2015.
 */
public class FutureCatch {

    public static class FutCatch extends Actor<FutCatch> {

        public Future error(int num) {
            Promise res = new Promise();
            delayed( 500, () -> res.receive(null,"Error " +num) );
            return res;
        }

        public Future result(int num) {
            Promise res = new Promise();
            delayed( 500, () -> res.receive("Result "+num, null) );
            return res;
        }

    }

    @Test
    public void testFut() throws InterruptedException {
        AtomicBoolean res = new AtomicBoolean(true);
        final FutCatch futCatch = AsActor(FutCatch.class);
        futCatch.result(0)
            .map(r -> {
                System.out.println("" + r);
                return futCatch.result(1);
            })
            .map(r -> {
                System.out.println("" + r);
                return futCatch.result(2);
            })
            .map(r -> {
                System.out.println("" + r);
                return futCatch.error(1);
            })
            .map(r -> {
                System.out.println(""+r);
                return futCatch.result(3);
            })
            .map(r -> {
                System.out.println(""+r);
                return futCatch.result(4);
            })
            .catchError(error -> {
                System.out.println("catched "+error);
            })
            .then((r, e) -> System.out.println("done"));
        Thread.sleep(40000);
    }

}
