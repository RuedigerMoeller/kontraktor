package examples.http4k;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.reactivestreams.EventSink;
import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.http.Http4K;

import java.util.Iterator;

/**
 * Created by ruedi on 11/07/15.
 */
public class SimpleServerWithWS {


    public static void main(String[] args) throws InterruptedException {

        KxPublisher<Long> timer = KxReactiveStreams.get().produce(new Iterator<Long>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                return System.currentTimeMillis();
            }
        });

        KxPublisher<Long> publisher = timer.lossy();
        timer.async().subscribe( (r,e) -> System.out.println(r) );

//        Http4K.Build("localhost", 8080)
//            .websocket("/ws/ms", (Actor) publisher)
//                .build()
//            .websocket("/ws/seconds", (Actor) publisher.asyncMap(i -> i / 1000))
//                .build()
//            .websocket("/ws/minutes", (Actor) publisher.asyncMap(i -> i / 1000 / 60))
//                .build()
//            .build();

    }

}
