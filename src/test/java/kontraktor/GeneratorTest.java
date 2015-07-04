package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.util.PromiseLatch;

import java.util.function.BiConsumer;

/**
 * Created by ruedi on 03/05/15.
 */
public class GeneratorTest {

    public static class Generator extends Actor<Generator> {

        public void generate( long interval, Callback<int[]> iterator ) {
            for ( int i = 0; i < 5; i++ ) {
                for ( int ii = 0; ii < 5; ii++ ) {
                    iterator.stream(new int[] {i,ii});
                    yield(interval);
                }
            }
            iterator.finish();
        }

        public IPromise run() {
            PromiseLatch finished = new PromiseLatch(5*5+1); // +1 == fin signal
            generate(100, (intarr, err) -> {
                if ( ! isErrorOrComplete(err) ) {
                    System.out.println("-> [" + intarr[0] + "," + intarr[1] + "]");
                }
                finished.countDown();
            });
            return finished.getPromise();
        }

    }

    @Test
    public void test() {
        Generator generator = Actors.AsActor(Generator.class);
        try {
            generator.run().await(1000*1000l);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
        try {
            generator.run().await(1*1000l);
            Assert.assertTrue(false);
        } catch (Exception e) {
            e.printStackTrace();
        }

        // test outside actor thread

        BiConsumer<Long,Boolean> outside = (timout, expectTO) -> {
            PromiseLatch finished = new PromiseLatch(5*5+1); // +1 == fin signal
            generator.generate(100, (intarr, err) -> {
                if ( ! Actor.isErrorOrComplete(err) ) {
                    System.out.println("-> [" + intarr[0] + "," + intarr[1] + "]");
                }
                finished.countDown();
            });
            try {
                finished.getPromise().await(timout);
                if ( expectTO )
                    Assert.assertTrue(false);
            } catch (Exception e) {
                e.printStackTrace();
                if ( ! expectTO )
                    Assert.assertTrue(false);
            }
        };

        outside.accept(1000l,true);
        outside.accept(100*1000l,false);
    }

}
