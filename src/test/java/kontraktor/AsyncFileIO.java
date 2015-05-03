package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.AsyncIO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 03/05/15.
 */
public class AsyncFileIO {

    static AtomicInteger count = new AtomicInteger();

    public static class IOUsingActor extends Actor<IOUsingActor> {

        public IPromise $readFile(String path) {
            Promise resultSignal = new Promise();
            AsyncIO.get().streamFile(path, 100000, (recbuff, err) -> {
                if (Thread.currentThread() != __currentDispatcher)
                    throw new RuntimeException("Ba !");
                if (recbuff != null) {
                    count.incrementAndGet();
                    System.out.println("chunk:" + recbuff + " " + System.currentTimeMillis());
                } else {
                    // error or finish
                    resultSignal.complete();
                    count.incrementAndGet();
                }
            });
            return resultSignal;
        }

    }

    @Test
    public void testRead() throws IOException, InterruptedException {

        FileOutputStream fout = null;
        String finam = "/tmp/test.data";
        try {
            byte b[] = "Dies ist ein Test".getBytes();
            fout = new FileOutputStream(finam);
            for ( int i = 0; i < 10000997; i++ ) {
                fout.write(b);
            }
        } finally {
            if ( fout != null )
                fout.close();
        }
        System.out.println("finished writing " + new File(finam).length());

        IOUsingActor tester = Actors.AsActor(IOUsingActor.class);
        try {
            tester.$readFile(finam).await(2000); // 100 seconds
            tester.$stop();
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
        Assert.assertTrue(count.get() == 1702);
    }

}
