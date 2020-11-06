package bench;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.remoting.base.RemoteRefPolling;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.websockets.WebSocketConnectable;
import org.nustaq.kontraktor.util.Log;

import java.util.concurrent.atomic.AtomicInteger;

public class BenchActorClient {

    public static void main(String[] args) {
        BackOffStrategy.SLEEP_NANOS = 1*1000*1000;
        RemoteRefPolling.EMPTY_Q_BACKOFF_WAIT_MILLIS = 1;

        int numMsg = 500_000;
        int numRep = 5;

        TCPConnectable con = new TCPConnectable(BenchActor.class, "localhost", 8181);
        BenchActor benchActorRemote = (BenchActor) con.connect().await();

        bench(numMsg, numRep, benchActorRemote);

        System.out.println("WS -------------------------------------");
        WebSocketConnectable conWS = new WebSocketConnectable(BenchActor.class, "ws://localhost:8182/ws");
        BenchActor benchActorRemoteWS = (BenchActor) conWS.connect( (r,e) -> {
            System.out.println("disconnected");
        }, (x) -> {
            System.out.println("disconnected 1");
        }).await();

        bench(numMsg, numRep, benchActorRemoteWS);

        System.out.println("HTLP -------------------------------------");
        HttpConnectable conLP = new HttpConnectable(BenchActor.class, "http://localhost:8182/htlp");
        BenchActor benchActorRemoteLP = (BenchActor) conLP.connect().await();

        bench(numMsg, numRep, benchActorRemoteLP);

        System.out.println("done");
    }

    private static void bench(int numMsg, int numRep, BenchActor benchActorRemote) {
        for (int i = 0; i < numRep; i++)
            reqRespBench(benchActorRemote, numMsg).await(60_000);
        for (int i = 0; i < numRep; i++)
            reqBench(benchActorRemote, numMsg).await(60_000);
    }

    private static IPromise reqBench(BenchActor benchActorRemote, int numMsg) {
        Promise p = new Promise<>();
        long now = System.currentTimeMillis();
        for( int i = 0; i < numMsg; i++ ) {
            int finalI = i;
            benchActorRemote.noEcho(i);
        }
        Integer await = (Integer) benchActorRemote.dummy().await(60_000);
        long dur = System.currentTimeMillis() - now;
        System.out.println("REQBENCH TIME: "+ dur +" "+(numMsg/dur*1000)+" per second "+await);
        p.resolve();
        return p;
    }

    private static IPromise reqRespBench(BenchActor benchActorRemote, int numMsg) {
        Promise p = new Promise<>();
        long now = System.currentTimeMillis();
        AtomicInteger responseCount = new AtomicInteger();
        for( int i = 0; i < numMsg; i++ ) {
            int finalI = i;
            benchActorRemote.echo(i).then( (result, error) -> {
                if ( ((Integer)result).intValue() != finalI)
                    System.out.println("Error");
                int respCount = responseCount.incrementAndGet();
//                System.out.println("repsonsecount "+responseCount.get());
                if ( respCount == numMsg-1 ) {
                    long dur = System.currentTimeMillis() - now;
                    System.out.println("ReqResp TIME: "+ dur +" "+(numMsg/dur*1000)+" per second");
                    p.resolve();
                }
            });
        }
        return p;
    }
}
