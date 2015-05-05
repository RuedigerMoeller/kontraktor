package kontraktor;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.asyncio.AsyncFile;
import org.nustaq.kontraktor.asyncio.AsyncFileIOEvent;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.file.StandardOpenOption;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by ruedi on 03/05/15.
 */
public class AsyncFileIO {

    static AtomicInteger count = new AtomicInteger();

    public static class IOUsingActor extends Actor<IOUsingActor> {

        public IPromise $readFile(String path) {
            try {
                AsyncFile fi = new AsyncFile(path);
                AsyncFileIOEvent event = new AsyncFileIOEvent(0, 0, ByteBuffer.allocate(100));
                do {
                    event = fi.read(event.getNextPosition(), 100, event.getBuffer()).await();
                    event.reset();
                } while( event.getNextPosition() >= 0 );
                fi.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

        public IPromise $readFileInputStream(String path) {
            try {
                BufferedReader bufferedReader = new BufferedReader( new InputStreamReader( new AsyncFile(path).asInputStream()), 5000);
                String line;
                while( (line=bufferedReader.readLine()) != null )
                    System.out.println(line);
                bufferedReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

        public IPromise $writeFileOutputStream(String path) {
            try {
                OutputStream outputStream = new AsyncFile(path,StandardOpenOption.WRITE, StandardOpenOption.CREATE).asOutputStream();
                for ( int i = 0; i < 997; i++ ) {
                    byte b[] = (""+i+" Dies ist ein Test\n").getBytes();
                    outputStream.write(b);
                }
                outputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

        public IPromise $writeFile(String path) {
            try {
                AsyncFile fi = new AsyncFile(path, StandardOpenOption.WRITE, StandardOpenOption.CREATE);
                ByteBuffer buff = ByteBuffer.allocate(100);
                AsyncFileIOEvent event = new AsyncFileIOEvent(0,0,buff);
                for ( int i = 0; i < 997; i++ ) {
                    byte b[] = (""+i+" Dies ist ein Test\n").getBytes();
                    buff.put(b);
                    buff.flip();
                    event = fi.write(event.getNextPosition(),buff).await();
                    event.reset();
                }
                fi.close();
            } catch (IOException e) {
                Actors.throwException(e);
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

    @Test
    public void testAsyncFile() throws IOException {
        count.set(0);
        String finam = "/tmp/test.data";
        IOUsingActor tester = Actors.AsActor(IOUsingActor.class);
        tester.$writeFile(finam).await();
        System.out.println("finished writing " + new File(finam).length());
        tester.$readFile(finam).await();
        tester.$readFileFully(finam).await();
        tester.$writeFileOutputStream(finam).await();
        tester.$readFileInputStream(finam).await();
        tester.$stop();
    }


}
