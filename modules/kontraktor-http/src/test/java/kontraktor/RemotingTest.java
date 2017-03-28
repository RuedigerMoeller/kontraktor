package kontraktor;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.remoting.base.ActorServer;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.http.HttpObjectSocket;
import org.nustaq.kontraktor.remoting.http.HttpPublisher;
import org.nustaq.kontraktor.remoting.tcp.NIOServerConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPServerConnector;
import org.nustaq.kontraktor.remoting.websockets.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.remoting.websockets._JSR356ServerConnector;
import org.nustaq.kontraktor.util.RateMeasure;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.LockSupport;

/**
 * Created by ruedi on 10/05/15.
 */
public class RemotingTest {

    public static final int Q_SIZE = 256_000;
    private static final boolean FAT_ARGS = false;
    private static final boolean ONLY_PROM = false;
    static AtomicInteger errors = new AtomicInteger();
    static boolean checkSequenceErrors = true;
    static boolean spore = false;
//    static int NUM_MSG = 15_000_000;
    static int NUM_MSG = 5_000_000;

    public static class RemotingTestService extends Actor<RemotingTestService> {

        public IPromise<String> promise(String s) {
            return new Promise<>(s+" "+s);
        }

        public void callback( Callback<String> cb ) {
            cb.stream("A");
            cb.stream("B");
            cb.stream("C");
            cb.finish();
        }

        public void spore( Spore<Integer,Integer> spore ) {
            spore.remote(1);
            spore.remote(2);
            spore.remote(3);
            spore.remote(4);
            spore.finish();
        }

        RateMeasure measure = new RateMeasure("calls",1000).print(true);
        int callCount = 0;
        public void benchMarkVoid(int someVal) {
            measure.count();
            callCount++;
            if ( callCount % 100_000 == 0 ) {
                System.out.println("CC "+callCount);
            }
        }

        int prev = -1;
        public IPromise<Integer> benchMarkPromise(int someVal) {
            measure.count();
            if ( checkSequenceErrors && someVal != prev+1 ) {
                errors.incrementAndGet();
                System.out.println("error: received "+someVal+" curr:"+prev);
            }
            prev = someVal;
            return new Promise<>(someVal);
        }

        public void benchMarkFatVoid(Object someVal) {
            measure.count();
        }

        public IPromise<Object> benchMarkFatPromise(Object someVal) {
            measure.count();
            return new Promise<>(someVal);
        }
    }

    // fixme: add connect-from-actor tests
    // fixme: add minbin tests
    // fixme: increase basic test coverage

    @Test @Ignore
    public void testWSJSR() throws Exception {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher = _JSR356ServerConnector.Publish(service, "ws://localhost:8081/ws", null).await();
        RemotingTestService client = (RemotingTestService)
            new WebSocketConnectable(RemotingTestService.class, "ws://localhost:8081/ws")
                .connect()
                .await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }


    @Test
    public void testHttp() throws Exception {
        Coding coding = new Coding(SerializerType.FSTSer);
        runtHttp(coding);
    }

    @Test
    public void testHttpJson() throws Exception {
        Coding coding = new Coding(SerializerType.Json);
        runtHttp(coding);
    }

    public void runtHttp(Coding coding) throws InterruptedException {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher =
            new HttpPublisher(service, "localhost", "/lp", 8082)
                .coding(coding)
                .publish()
                .await();
        RemotingTestService client = (RemotingTestService)
            new HttpConnectable(RemotingTestService.class, "http://localhost:8082/lp")
                .coding(coding)
                .connect()
                .await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testHttpMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher =
            new HttpPublisher(service, "localhost", "/lp", 8082)
                .publish()
                .await();
        RemotingTestService client = (RemotingTestService)
            new HttpConnectable(RemotingTestService.class, "http://localhost:8082/lp")
                .connect()
                .await(9999999);
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runWithClient(client, latch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testWS() throws Exception {
        Coding coding = new Coding(SerializerType.FSTSer);
        runWS(coding);
    }

    @Test
    public void testWSJson() throws Exception {
        Coding coding = new Coding(SerializerType.JsonNoRef);
        runWS(coding);
    }

    public void runWS(Coding coding) throws InterruptedException {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher = new WebSocketPublisher(service, "localhost", "/ws", 8081).coding(coding).publish().await();
        RemotingTestService client = (RemotingTestService)
            new WebSocketConnectable(RemotingTestService.class, "ws://localhost:8081/ws")
                .coding(coding)
                .connect()
                .await(9999999);
        CountDownLatch latch = new CountDownLatch(1);
        runWithClient( client, latch );
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testWSMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher = new WebSocketPublisher(service, "localhost", "/ws", 8081).publish().await();
        RemotingTestService client = (RemotingTestService)
            new WebSocketConnectable(RemotingTestService.class, "ws://localhost:8081/ws")
                .connect()
                .await(9999999);

        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runWithClient(client, latch);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testNIO() throws Exception {
        Coding coding = new Coding(SerializerType.FSTSer);
        runNio(coding);
    }

    @Test
    public void testNIOJSon() throws Exception {
        Coding coding = new Coding(SerializerType.Json);
        runNio(coding);
    }

    public void runNio(Coding coding) throws Exception {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, coding).await();
        CountDownLatch latch = new CountDownLatch(1);
        runnitTCP(latch,coding);
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testNIOMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher = NIOServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(latch, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testBlocking() throws Exception {
        checkSequenceErrors = true;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        Coding coding = new Coding(SerializerType.FSTSer);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, coding).await();
        CountDownLatch latch = new CountDownLatch(1);
        runnitTCP(latch, coding);
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    @Test
    public void testBlockingMany() throws Exception {
        checkSequenceErrors = false;
        RemotingTestService service = Actors.AsActor(RemotingTestService.class, Q_SIZE);
        ActorServer publisher = TCPServerConnector.Publish(service, 8081, null).await();
        ExecutorService exec = Executors.newCachedThreadPool();
        CountDownLatch latch = new CountDownLatch(10);
        for ( int i = 0; i < 10; i++ )
        {
            exec.execute(() -> {
                try {
                    runnitTCP(latch, null);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
        }
        latch.await();
        Thread.sleep(2000); // wait for outstanding callbacks
        publisher.close();
    }

    public void runnitTCP(CountDownLatch l, Coding coding) throws Exception {
        RemotingTestService client = (RemotingTestService)
            new TCPConnectable()
                .actorClass(RemotingTestService.class)
                .host("localhost")
                .port(8081)
                .coding(coding)
                .connect()
                .await();
        runWithClient(client,l);
        Thread.sleep(2000); // wait for outstanding callbacks
        client.close();
    }

    public static class Pojo implements Serializable{
        String name;
        String preName;
        int age;
        String sex;
        double field;

        String name1;
        String preName1;
        int age1;
        String sex1;
        double field1;

        String name2;
        String preName2;
        int age2;
        String sex2;
        double field2;

        String name3;
        String preName3;
        int age3;
        String sex3;
        double field3;

        public Pojo(String name, String preName, int age, String sex, double field, String name1, String preName1, int age1, String sex1, double field1, String name2, String preName2, int age2, String sex2, double field2, String name3, String preName3, int age3, String sex3, double field3) {
            this.name = name;
            this.preName = preName;
            this.age = age;
            this.sex = sex;
            this.field = field;
            this.name1 = name1;
            this.preName1 = preName1;
            this.age1 = age1;
            this.sex1 = sex1;
            this.field1 = field1;
            this.name2 = name2;
            this.preName2 = preName2;
            this.age2 = age2;
            this.sex2 = sex2;
            this.field2 = field2;
            this.name3 = name3;
            this.preName3 = preName3;
            this.age3 = age3;
            this.sex3 = sex3;
            this.field3 = field3;
        }
    }

    protected void runWithClient(RemotingTestService client, CountDownLatch l) throws InterruptedException {
        Assert.assertTrue("Hello Hello".equals(client.promise("Hello").await(999999)));

        AtomicInteger replyCount = new AtomicInteger(0);
        if ( spore ) {
            ArrayList<Integer> sporeResult = new ArrayList<>();
            Promise sporeP = new Promise();
            client.spore(new Spore<Integer, Integer>() {
                @Override
                public void remote(Integer input) {
                    stream(input);
                }
            }.setForEach((res, e) -> {
                System.out.println("spore res " + res);
                sporeResult.add(res);
            }).onFinish(() -> {
                System.out.println("Finish");
                sporeP.complete();
            }));
            sporeP.await(30_000);
            Assert.assertTrue(sporeResult.size() == 4);
        }

        System.out.println("one way performance");
        for ( int i = 0; !ONLY_PROM && i < NUM_MSG; i++ ) {
            if ( FAT_ARGS ) {
                Object map = createPojo(i);
//                System.out.println(FSTConfiguration.getDefaultConfiguration().asByteArray(map).length);
                client.benchMarkFatVoid(map);
            } else {
                client.benchMarkVoid(i);
            }
        }
        client.ping().await(60_000);
        System.out.println("two way performance");
        errors.set(0);
        boolean seq[] = new boolean[NUM_MSG];
        for ( int i = 0; i < NUM_MSG; i++ ) {
            if ( i%1_000_000==0 )
                System.out.println("sent "+i+" "+replyCount.get());
            while ( i - replyCount.get() > 200_000 ) { // FIXME: remoteref registry should do this, but how to handle unanswered requests ?
                LockSupport.parkNanos(1);
            }
            if ( FAT_ARGS ) {
                client.benchMarkFatPromise(createPojo(i)).then(s -> {
                    replyCount.incrementAndGet();
                });
            } else {
                client.benchMarkPromise(i).then(s -> {
                    replyCount.incrementAndGet();
                    if (seq[s])
                        errors.incrementAndGet();
                    seq[s] = true;
                });
            }
        }
        Thread.sleep(2000);
        if (replyCount.get() != NUM_MSG) {
            System.out.println("extend wait ..");
            Thread.sleep(HttpObjectSocket.LP_TIMEOUT*2);
        }
        for (int i = 0; ! FAT_ARGS && i < seq.length; i++) {
            boolean b = seq[i];
            if ( !b ) {
                System.out.println("missing:"+i);
                errors.incrementAndGet();
            }
        }
        System.out.println("done "+Thread.currentThread()+" "+replyCount);
        junit.framework.Assert.assertTrue(replyCount.get() == NUM_MSG);
        junit.framework.Assert.assertTrue(errors.get() == 0);
        l.countDown();
    }

    private Object createPojo(int i) {
        return new Object[] {
            newPojo(i/1),
            newPojo(i/2),
            newPojo(i/3),
            newPojo(i/4),
            newPojo(i/5),
            newPojo(i/6),
            newPojo(i/7),
            newPojo(i/8),
            newPojo(i/9),
            newPojo(i/10),
        };
    }

    private Pojo newPojo(int i) {return new Pojo("Some Name","Some Prename", i, "M", i*1.2,"NameNumber2","Prename1",i*2,"W",1.323,"BLASDASD","oaisjd",43534+i,"?",234.33,"sldfjlsdfk","lsdkfjsdf",13+i,"MW",1234.123+i);}

}
