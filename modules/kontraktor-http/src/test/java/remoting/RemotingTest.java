package remoting;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.ConnectableActor;
import org.nustaq.kontraktor.remoting.base.TrafficMonitor;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;
import org.nustaq.kontraktor.remoting.http.undertow.WebSocketPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.remoting.tcp.TCPPublisher;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class RemotingTest {

    @Test
    public void test() {
        RemotingTA serv = Actors.AsActor(RemotingTA.class);

        // websocket
        WebSocketPublisher pub = new WebSocketPublisher()
            .facade(serv)
            .hostName("0.0.0.0")
            .urlPath("/websocket")
            .port(7777)
            .serType(SerializerType.FSTSer);
        pub.setTrafficMonitor(new TrafficMonitorImpl("pub"));
        pub.publish().await();
        ((TrafficMonitorImpl) pub.getTrafficMonitor()).printAndClear();

        WebSocketConnectable con = new WebSocketConnectable()
            .actorClass(RemotingTA.class)
            .url("ws://localhost:7777/websocket");
        fromRemote(con);


        // TCP NIO
        TCPNIOPublisher tcpnioPublisher = new TCPNIOPublisher(serv, 7778);
        tcpnioPublisher.setTrafficMonitor(new TrafficMonitorImpl("tcpnioPublisher"));
        tcpnioPublisher.publish().await();
        fromRemote(new TCPConnectable(RemotingTA.class, "localhost", 7778));
        ((TrafficMonitorImpl) tcpnioPublisher.getTrafficMonitor()).printAndClear();


        // TCP Sync
        TCPPublisher tcpPublisher = new TCPPublisher(serv, 7780);
        tcpPublisher.setTrafficMonitor(new TrafficMonitorImpl("tcpPublisher"));
        tcpPublisher.publish().await();
        fromRemote(new TCPConnectable(RemotingTA.class, "localhost", 7780));
        ((TrafficMonitorImpl) tcpPublisher.getTrafficMonitor()).printAndClear();


        // Http-Longpoll
        HttpPublisher httpPublisher = new HttpPublisher(serv, "0.0.0.0", "/httpapi", 7779);
        httpPublisher.setTrafficMonitor(new TrafficMonitorImpl("httpPublisher"));
        httpPublisher.publish().await();
        fromRemote(new HttpConnectable(RemotingTA.class, "http://localhost:7779/httpapi"));
        ((TrafficMonitorImpl) httpPublisher.getTrafficMonitor()).printAndClear();
    }

    @Ignore
    @Test
    public void testLimit() throws InterruptedException {
        RemotingTA serv = Actors.AsActor(RemotingTA.class);
        // Http-Longpoll
        new HttpPublisher(serv,"0.0.0.0","/httpapi",7779).publish().await();

        RemotingTA client = (RemotingTA) new HttpConnectable(RemotingTA.class, "http://localhost:7779/httpapi").connect().await();
        for ( int i=0; i < 200; i++ ) {
            client.yes().then( (r,e)-> System.out.println(r+" "+e));
            Thread.sleep(10_000);
        }
    }

    private void fromRemote(ConnectableActor con) {
        RemotingTA remote = (RemotingTA) con.connect().await();


        IPromise fin = new Promise();
        AtomicInteger count = new AtomicInteger();
        Integer expect = remote.sayHello(10, (str, err) -> {
            if (Actors.isCont(err)) {
                System.out.println("received:" + str);
                count.incrementAndGet();
            } else {
                fin.complete();
            }
        }).await();

        fin.await();

        Assert.assertTrue(expect.intValue() == count.intValue() );
    }

    public static class TrafficMonitorImpl implements TrafficMonitor {

        public static boolean VERBOSE = true;
        public static boolean IS_ACTIVE = true;

        private final String name;

        private ConcurrentHashMap<String, Long> map = new ConcurrentHashMap<>();

        public TrafficMonitorImpl(String name) {
            this.name = name;
        }

        @Override
        public void requestReceived(int size, String sid, String path) {
            add(sid + " | " + (path == null ? "unknown path" : path) + " | req", size);
            add(sid + " | sum req", size);
        }

        @Override
        public void responseSend(int size, String sid, String path) {
            add(sid + " | " +  (path == null ? "unknown path" : path) + " | res", size);
            add(sid + " | sum res", size);
        }

        public void add(String identifier, long bytesCountToBeAdded) {
            if ( !IS_ACTIVE ) {
                return;
            }

            if ( bytesCountToBeAdded < 0 ) {
                throw new IllegalArgumentException("bytes count to be added must be greater zero");
            }

            map.compute(identifier, (String id, Long storedBytesCount) -> {
                if (storedBytesCount == null) {
                    storedBytesCount = 0L;
                }

                long result = storedBytesCount + bytesCountToBeAdded;

                if (VERBOSE) {
                    System.out.println("TrafficMonitor(" + name + ") - id '" + id + "': add " + bytesCountToBeAdded + ", total: " + result);
                }

                return result;
            });
        }

        public void printAndClear() {
            System.out.println("Stats for TrafficMonitor(" + name + "):");
            map.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .forEachOrdered(e -> {
                    System.out.println(" - " + e.getKey() + ": " + e.getValue() + " bytes");
                });

            map = new ConcurrentHashMap<>();
        }
    }
}
