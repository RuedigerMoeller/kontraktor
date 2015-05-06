package kontraktor;

import jdk.nashorn.internal.ir.annotations.Ignore;
import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.asyncio.AsyncFile;
import org.nustaq.kontraktor.asyncio.AsyncServerSocket;
import org.nustaq.kontraktor.asyncio.AsyncServerSocketConnection;
import org.nustaq.kontraktor.asyncio.QueuingAsyncSocketConnection;
import org.nustaq.kontraktor.util.RateMeasure;
import org.nustaq.net.TCPObjectServer;
import org.nustaq.net.TCPObjectSocket;
import org.nustaq.offheap.BinaryQueue;
import org.nustaq.serialization.FSTConfiguration;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

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
                sock.connect( 8080, (key,con) -> new QueuingAsyncSocketConnection(key, con) {
                    @Override
                    public void dataReceived(BinaryQueue q) {
                        System.out.println("size "+q.available());
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        int receiveCount = 0;
        public IPromise $serve2() {
            receiveCount = 0;
            RateMeasure ms = new RateMeasure("async receive object");
            AsyncServerSocket sock = new AsyncServerSocket();
            FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
            try {
                sock.connect( 8080, (key,con) -> new QueuingAsyncSocketConnection(key, con) {

                    @Override
                    public void dataReceived(BinaryQueue q) {
                        while ( q.available() > 4 ) {
                            int len = q.readInt();
                            if ( q.available() >= len ) {
                                byte[] bytes = q.readByteArray(len);
                                receivedObject(conf.asObject(bytes));
                            } else {
                                q.back(4);
                                break;
                            }
                        }
                    }

                    private void receivedObject(Object o) {
                        // probably dispatch to 'worker actors' scheduled on different thread
                        ms.count();
                        receiveCount++;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

        public IPromise $serve3() {
            receiveCount = 0;
            RateMeasure ms = new RateMeasure("async receive object");
            AsyncServerSocket sock = new AsyncServerSocket();
            FSTConfiguration conf = FSTConfiguration.createDefaultConfiguration();
            try {
                sock.connect( 8080, (key,con) -> new QueuingAsyncSocketConnection(key, con) {

                    @Override
                    public void dataReceived(BinaryQueue q) {
                        while ( q.available() > 4 ) {
                            int len = q.readInt();
                            if ( q.available() >= len ) {
                                byte[] bytes = q.readByteArray(len);
                                receivedObject(conf.asObject(bytes));
                            } else {
                                q.back(4);
                                break;
                            }
                        }
                    }

                    private void receivedObject(Object o) {
                        if ( o instanceof File) {
                            try {
                                AsyncFile fi = new AsyncFile(((File) o).getAbsolutePath());
                                InputStream inputStream = fi.asInputStream();
                                byte tmp[] = new byte[16000];
                                int read = 0;
                                int sum = 0;
                                while( (read=inputStream.read(tmp)) >= 0 ) {
                                    write(tmp,0,read);
                                    sum+=read;
                                }
                                inputStream.close();
                                System.out.println("wrote file len " + sum + " writeQ "+writeQueue);
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                        receiveCount++;
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            return complete();
        }

        public IPromise<Integer> $getReceiveCount() {
            return resolve(receiveCount);
        }
    }

    @Test @Ignore
    public void plain() throws InterruptedException {
        Actors.AsActor(TA.class).$serve();
        Thread.sleep(1000000l);
    }

    @Test @Ignore
    public void queued() throws InterruptedException {
        TA ta = Actors.AsActor(TA.class);
        ta.$serve1();
        Thread.sleep(1000000l);
    }

    @Test
    public void serial() throws Exception {
        TA ta = Actors.AsActor(TA.class);
        ta.$serve2().await();
        ExecutorService executorService = Executors.newCachedThreadPool();
        HashMap testMap = new HashMap();
        int MSG_COUNT = 10_000_000;
        int NUM_CLIENTS = 100;
        testMap.put("pok",13);
        testMap.put("yes","no");
        testMap.put("true", "maybe");
        for ( int ii = 0; ii < NUM_CLIENTS; ii++ ) {
            final int finalIi = ii;
            executorService.execute(()->{
                TCPObjectSocket sock = null;
                try {
                    sock = new TCPObjectSocket("localhost",8080);
                    for ( int i = 0; i < MSG_COUNT/NUM_CLIENTS; i++ ) {
                        sock.writeObject(testMap);
                        sock.flush();
                    }
                    sock.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.sleep(1000);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.DAYS);
        Thread.sleep(1000);
        Integer count = ta.$getReceiveCount().await();
        System.out.println("COUNT " + count);
        Assert.assertTrue(count == MSG_COUNT);
        ta.$stop();

    }

    @Test
    public void serialSync() throws Exception {

        AtomicInteger COUNT = new AtomicInteger(0);

        TCPObjectServer tcpObjectServer = new TCPObjectServer(8080);
        RateMeasure measure = new RateMeasure("count");
        tcpObjectServer.start((client) -> {
                try {
                    while (true) {
                        Object request = client.readObject();
                        if (request == null)
                            return; // connection closed
                        COUNT.incrementAndGet();
                        measure.count();
                    }
                } catch (EOFException eof) {
                    //e.printStackTrace();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        );

        Thread.sleep(1000);
        HashMap testMap = new HashMap();
        testMap.put("pok",13);
        testMap.put("yes","no");
        testMap.put("true", "maybe");

        ExecutorService executorService = Executors.newCachedThreadPool();
        int MSG_COUNT = 10_000_000;
        int NUM_CLIENTS = 100;
        for ( int ii = 0; ii < NUM_CLIENTS; ii++ ) {
            final int finalIi = ii;
            executorService.execute(()->{
                TCPObjectSocket sock = null;
                try {
                    sock = new TCPObjectSocket("localhost",8080);
//                    System.out.println("start "+finalIi);
                    for ( int i = 0; i < MSG_COUNT/NUM_CLIENTS; i++ ) {
                        sock.writeObject(testMap);
                        sock.flush();
                    }
                    sock.close();
//                    System.out.println("finished "+ finalIi);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        Thread.sleep(3000);
        executorService.shutdown();
        executorService.awaitTermination(10, TimeUnit.DAYS);
        Thread.sleep(3000);
        System.out.println("COUNT " + COUNT.get());
        Assert.assertTrue(COUNT.get() == MSG_COUNT);

    }

    @Test
    public void writeTest() throws IOException {
        File f = new File("/tmp/data.test");
        int NUMINT = 10_000_000;
        if ( ! f.exists() ) {
            FileOutputStream fout = new FileOutputStream(f);
            DataOutputStream dout = new DataOutputStream(fout);
            for (int i = 0; i < NUMINT; i++) {
                dout.writeInt(i);
            }
            fout.close();
        }

        System.out.println("file len " + f.length());

        TA ta = Actors.AsActor(TA.class);
        ta.$serve3().await();
        RateMeasure intFreq = new RateMeasure("int per sec");

        TCPObjectSocket sock = null;
        try {
            sock = new TCPObjectSocket("localhost",8080);
            sock.writeObject(f);
            sock.flush();
            byte res[] = new byte[1024];
            int count = 0;
            int read;
            InputStream in = sock.getIn();
            DataInputStream din = new DataInputStream(in);
            while( (read = din.readInt()) >= 0 ) {
                Assert.assertTrue(read == count);
                count++;
                if ( count == NUMINT )
                    break;
//                if ( count % 10000 == 0 )
//                    System.out.println("read int "+count);
                intFreq.count();
            }
            in.close();
            System.out.println("received "+count+" KB");
        } catch (Exception e) {
            e.printStackTrace();
        }
        ta.$stop();
    }

}
