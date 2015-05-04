package kontraktor;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.AsyncFile;
import org.nustaq.kontraktor.util.AsyncIO;
import org.nustaq.kontraktor.util.AsyncFileIOEvent;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 03/05/15.
 */
public class AsyncFileIO {

    static AtomicInteger count = new AtomicInteger();

    public static class IOUsingActor extends Actor<IOUsingActor> {

        public IPromise $streamFile(String path) {
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

        public IPromise $readFile(String path) {
            try {
                AsyncFile fi = new AsyncFile(path);
                AsyncFileIOEvent event = new AsyncFileIOEvent(0, 0, ByteBuffer.allocate(100));
                do {
                    event = fi.read(event.getNextPosition(), 100, event.getBuffer()).await();
                    System.out.println(event+" read: "+new String(event.copyBytes(),0));
                    event.reset();
                } while( event.getNextPosition() >= 0 );
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

        public IPromise $readFileFully(String path) {
            try {
                AsyncFile fi = new AsyncFile(path);
                AsyncFileIOEvent event = fi.readFully().await();
                System.out.println("FULLY: "+event+" "+new String(event.copyBytes(),0));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

    }

    static class AsyncFileReader extends Reader {

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return 0;
        }

        @Override
        public void close() throws IOException {

        }
    }

    @Test
    public void testRead() throws IOException {
        count.set(0);
        FileOutputStream fout = null;
        String finam = "/tmp/test.data";
        try {
            fout = new FileOutputStream(finam);
            for ( int i = 0; i < 997; i++ ) {
                byte b[] = (""+i+" Dies ist ein Test").getBytes();
                fout.write(b);
            }
        } finally {
            if ( fout != null )
                fout.close();
        }
        System.out.println("finished writing " + new File(finam).length());
        IOUsingActor tester = Actors.AsActor(IOUsingActor.class);
        tester.$readFile(finam).await();
        tester.$readFileFully(finam).await();
        tester.$stop();
    }

    @Test
    public void testStreamRead() throws IOException, InterruptedException {
        count.set(0);
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
            tester.$streamFile(finam).await(2000); // 100 seconds
            tester.$stop();
            System.out.println("Done");
        } catch (Exception e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }
        Assert.assertTrue(count.get() == 1702);
    }

}
