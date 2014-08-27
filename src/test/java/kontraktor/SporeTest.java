package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.Spore;

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

        public void $query( Spore<String,Object> query ) {
            data.forEach( (string) -> {
                query.remote( string );
            });
            query.finished();
        }

    }

    static AtomicInteger res = new AtomicInteger(0);

    public static class Caller extends Actor<Caller> {

        public void $testSpore( DataHolder data, String subs ) {
            Thread.currentThread().setName("Caller");
            checkThread();
            data.$query(
                new Spore<String, Object>() {

                    String toSearch; // data required remotely

                    {
                        toSearch = subs; // capture data from current context
                    }

                    // the method executed remotely
                    public void remote(String input) {
                        assert  Thread.currentThread().getName().equals("DataHolder");
                        if (input.indexOf(subs) >= 0) {
                            receiveResult(input, Callback.CONT);
                        }
                    }

                    // here the objects emmitted by the remote code come in again
                    public void local(Object result, Object error) {
                        assert  Thread.currentThread().getName().equals("Caller");
                        int i = res.incrementAndGet();
                        System.out.println("result "+i+":" + result + " err:" + error);
                        checkThread();
                    }

                }
            );
        }
    }

    @Test
    public void testSpore() throws InterruptedException {
        DataHolder data = Actors.AsActor(DataHolder.class);
        data.$init(500);
        Caller caller = Actors.AsActor(Caller.class);
        caller.$testSpore(data, "maybe");
        Thread.sleep(1000);
        Assert.assertTrue( res.get() == 73 );
    }
}
