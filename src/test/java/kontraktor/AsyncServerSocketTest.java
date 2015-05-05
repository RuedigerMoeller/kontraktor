package kontraktor;

import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.asyncio.AsyncServerSocket;
import org.nustaq.kontraktor.asyncio.AsyncServerSocketConnection;
import org.nustaq.kontraktor.asyncio.RingbufferingAsyncSSConnection;
import org.nustaq.net.TCPObjectSocket;
import org.nustaq.offheap.BinaryQueue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.HashMap;

/**
 * Created by moelrue on 5/5/15.
 */
public class AsyncServerSocketTest {

    public static class TA extends Actor<TA> {

        public void $serve() {
            AsyncServerSocket sock = new AsyncServerSocket();
            try {
                sock.connect( 8080, (key,con) -> new AsyncServerSocketConnection(key, con) {
                    @Override
                    public void dataReceived(ByteBuffer buf) {
                        System.out.println(new String(buf.array(),0,0,buf.limit()));
                        System.out.println("len:"+buf.limit());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void $serve1() {
            AsyncServerSocket sock = new AsyncServerSocket();
            try {
                sock.connect( 8080, (key,con) -> new RingbufferingAsyncSSConnection(key, con) {
                    @Override
                    public void dataReceived(BinaryQueue q) {
                        System.out.println("size "+q.size());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void $serve2() {
            AsyncServerSocket sock = new AsyncServerSocket();
            try {
                sock.connect( 8080, (key,con) -> new RingbufferingAsyncSSConnection(key, con) {
                    @Override
                    public void dataReceived(BinaryQueue q) {
                        System.out.println("size "+q.size());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    @Test
    public void plain() throws InterruptedException {
        Actors.AsActor(TA.class).$serve();
        Thread.sleep(1000000l);
    }

    @Test
    public void queued() throws InterruptedException {
        Actors.AsActor(TA.class).$serve1();
        Thread.sleep(1000000l);
    }

    @Test
    public void serial() throws Exception {
        Actors.AsActor(TA.class).$serve2();
        TCPObjectSocket sock = new TCPObjectSocket("localhost",8080);
        HashMap testMap = new HashMap();
        testMap.put("pok",13);
        testMap.put("yes","no");
        testMap.put("true", "maybe");

        for ( int i = 0; i < 1000; i++ ) {
            sock.writeObject(testMap);
            sock.flush();
            Thread.sleep(1000);
        }
        Thread.sleep(1000000l);
    }

}
