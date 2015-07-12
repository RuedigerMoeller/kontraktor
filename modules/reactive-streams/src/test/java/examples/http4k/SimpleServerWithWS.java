package examples.http4k;

import org.nustaq.kontraktor.reactivestreams.KxPublisher;
import org.nustaq.kontraktor.reactivestreams.KxReactiveStreams;
import org.nustaq.kontraktor.remoting.http.Http4K;

import java.util.Iterator;
import java.util.function.Function;

/**
 * Created by ruedi on 11/07/15.
 */
public class SimpleServerWithWS {

    static class Divider implements Function<Long,Long> {

        int divisor;
        int count = 0;

        public Divider(int divisor) {
            this.divisor = divisor;
        }

        @Override
        public Long apply(Long aLong) {
            if ( divisor == 60000 )
            {
                if ( count >= 60_000 ) {
                    System.out.println("MINUTE !");
                }
            }
            count++;
            if ( count < divisor ) {
                return null;
            }
            count = 0;
            return aLong/divisor;
        }
    }

    public static void main(String[] args) throws InterruptedException {

        KxPublisher<Long> timer = KxReactiveStreams.get().produce(new Iterator<Long>() {
            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public Long next() {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException e) {
                    //e.printStackTrace();
                }
                return System.currentTimeMillis();
            }
        });

        // timer is not 1:N capable
        // in addition streams with no clients would block
        // (slowest dominates, if no client => no consumer => everybody blocked)
        KxPublisher<Long> publisher = timer.lossy();

        Http4K.Build("localhost", 8080)
            .websocket("/ws/ms", publisher.asActor())
                .build()
            .websocket("/ws/seconds", publisher.map(new Divider(1000)).asActor())
                .build()
            .websocket("/ws/minutes", publisher.map(new Divider(1000 * 60)).asActor())
                .build()
            .build();

    }

}
