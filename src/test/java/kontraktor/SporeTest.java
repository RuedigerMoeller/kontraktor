package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Spore;
import org.nustaq.kontraktor.annotations.CallerSideMethod;

import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 26.08.14.
 */
public class SporeTest {

    public static class DataHolder extends Actor<DataHolder> {

        ArrayList<String> data = new ArrayList<>();

        public void $init( int sizeCollection ) {
            Thread.currentThread().setName("DataHolder");
            for ( int i=0; i < sizeCollection; i++ ) {
                data.add("Kontraktor is "+i+" times cooler than ice.");
                if ( (i%7) == 0 )
                    data.add("maybe");
                if ( (i%9) == 0 )
                    data.add("depends");
            }
        }

        public void $doQuery(Spore<String, Object> query) {
            data.forEach( string -> {
                query.remote( string );
            });
            query.finish();
        }

    }

    static AtomicInteger res = new AtomicInteger(0);

    public static class Caller extends Actor<Caller> {

        public void $testSpore( DataHolder data, String subs ) {
            Thread.currentThread().setName("Caller");
            checkThread();
            data.$doQuery(
                new Spore<String, Object>() {

                    String toSearch; // data required remotely

                    {
                        toSearch = subs; // capture data from current context
                    }

                    // the method executed remotely
                    public void remote(String input) {
                        assert Thread.currentThread().getName().equals("DataHolder");
                        if (input.indexOf(subs) >= 0) {
                            stream(input);
                        }
                    }

                } // result receiving executed locally
                .forEach((result, error) -> {
                    assert Thread.currentThread().getName().equals("Caller");
                    int i = res.incrementAndGet();
                    System.out.println("result " + i + ":" + result + " err:" + error);
                    checkThread();
                })
                .onFinish(() -> System.out.println("DONE"))
            );
            Thread t = Thread.currentThread();
            data.$submit( () -> {
                if ( t == Thread.currentThread() ) {
                    res.incrementAndGet();
                }
            });
        }
    }

    @Test
    public void testSpore() throws InterruptedException {
        DataHolder data = Actors.AsActor(DataHolder.class);
        data.$init(500);
        Caller caller = Actors.AsActor(Caller.class);
        caller.$testSpore(data, "maybe");
        Thread.sleep(2000);
        Assert.assertTrue( res.get() == 73 );
    }
}
