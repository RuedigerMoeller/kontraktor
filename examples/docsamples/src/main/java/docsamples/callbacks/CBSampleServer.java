package docsamples.callbacks;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;

public class CBSampleServer<T extends CBSampleServer> extends Actor<T> {

    // Actor method
    public void withCallbackRaw(String arg, Callback callback) {
        callback.complete("Hello", "CNT");
        callback.complete(arg,"CNT");
        callback.complete(null,null);
    }

    // identical but uses convenience methods
    public void withCallback(String arg, Callback callback) {
        callback.pipe("Hello");
        callback.pipe(arg);
        callback.finish();
    }

    public void timePing(int count, Callback<Long> cb) {
        if ( count <= 0 ) {
            cb.finish(); // ensure cleanup of internal mapping supporting remoting.
            return;
        }
        if ( cb.isTerminated() ) {
            System.out.println("a client terminated");
            return;
        }
        if ( ! isStopped() ) {
            cb.pipe(System.currentTimeMillis());
            delayed(1000L, () -> timePing(count-1, cb ) );
        }
    }

    public static void main(String[] args) {
        CBSampleServer actor = Actors.AsActor(CBSampleServer.class);
        new TCPNIOPublisher()
            .port(9090)
            .serType(SerializerType.FSTSer)
            .facade(actor)
            .publish( discact -> System.out.println("a client disconnected "+discact));
    }

}