package soa;

import io.undertow.server.HttpServerExchange;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class WebServer extends Actor<WebServer> {

    private ServiceWithWebServer myService;

    static int CHUNK_SIZE = 300_000;
    static int REPETITIONS = 20;

    private void runBenchMark() {
        // as bench uses blocking api (countdown latch), run it not on actor thread
        execInThreadPool( () -> {
            for (int ii = 0; ii < REPETITIONS; ii++) {
                long now = System.currentTimeMillis();
                CountDownLatch latch = new CountDownLatch(CHUNK_SIZE);
                for (int i = 0; i < CHUNK_SIZE; i++) {
                    myService.benchMe(i).then((r, e) -> latch.countDown());
                }
                try {
                    latch.await();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                long dur = System.currentTimeMillis() - now;
                System.out.println("time for "+CHUNK_SIZE+" is "+ dur +" ms; per sec:"+( (double)CHUNK_SIZE/dur * 1000 ));
            }
           return null;
        });
    }

    // example for handling a raw http request (see also init() )
    public void handleRunBench(HttpServerExchange httpServerExchange) {
        runBenchMark();
        httpServerExchange.getResponseSender().send("started benchmark");
    }

    public void init(ServiceWithWebServer myService ) {
        this.myService = myService;
        Http4K.Build( "localhost", 8089 )
            .fileRoot("/", "./webroot")
            .httpHandler("/runbench", httpServerExchange -> {
                httpServerExchange.dispatch(); // required as we are going to change threads ..
                self().handleRunBench(httpServerExchange);
            })
            .httpAPI("/api", self())
                .coding(new Coding(SerializerType.JsonNoRef))
                .buildHttpApi()
            .build();
        System.out.println("WebServer initialized on localhost:8089");
    }

}
