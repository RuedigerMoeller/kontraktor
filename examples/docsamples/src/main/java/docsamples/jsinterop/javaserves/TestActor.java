package docsamples.jsinterop.javaserves;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;
import org.nustaq.serialization.coders.Unknown;
import java.util.Arrays;

public class TestActor extends Actor<TestActor> {

    public IPromise<String> getName() {
        return new Promise("TestActor "+System.identityHashCode(this));
    }
    public void plain( String arg, int arg1 ) {
        System.out.println("plain "+arg+" "+arg1);
    }
    public IPromise<String> plainPromise(String arg, int arg1 ) {
        return new Promise("plainPromise " + arg + " " + arg1);
    }
    public IPromise<String> plainCallback(String arg, int arg1, Callback cb ) {
        cb.pipe(arg).pipe(arg1).finish();
        return new Promise<>("plainCallback " + arg + " " + arg1);
    }
    public IPromise<TestPojo> plainPojo(TestPojo in) {
        return new Promise(in);
    }
    public void plainUnknown(Unknown in) {
        System.out.println("unknown"+in);
    }
    public void simpleTypes( String[] arr, int[] iarr ) {
        System.out.println(Arrays.toString(arr));
        System.out.println(Arrays.toString(iarr));
    }
    public IPromise<TestActor> createAnotherOne(String name) {
        TestActor testActor = AsActor(TestActor.class,getScheduler()); // use same thread to run it
        return new Promise<>(testActor); // remoteref !!
    }

    ///////////////////////////////// startup /////////////////////////////////////////////////////////

    public static void main(String[] args) {
        new HttpPublisher(AsActor(TestActor.class),"localhost", "/test", 8888)
            .serType(SerializerType.JsonNoRef)
            .publish( x -> System.out.println("DISCONNECTED:"+x));
    }
}
