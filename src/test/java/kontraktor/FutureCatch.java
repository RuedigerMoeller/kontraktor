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

        public Future error() {
            return new Promise<>(null,"Error");
        }

        public Future result() {
            return new Promise<>("result",null);
        }

        public Future exc() {
            return new Promise<>(null, new RuntimeException());
        }

    }

    @Test
    public void testFut() {
        AtomicBoolean res = new AtomicBoolean(true);
        final FutCatch futCatch = AsActor(FutCatch.class);
        futCatch.error()
            .onResult( r -> {
                res.set(false);
                System.out.println("r1");
            })
            .then(futCatch.result())
            .onError(r -> System.out.println("ERR"));
    }

}
