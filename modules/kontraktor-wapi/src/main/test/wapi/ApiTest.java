package wapi;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.Http4K;
import org.nustaq.kontraktor.remoting.http.HttpPublisher;
import org.nustaq.kontraktor.remoting.service.DenialReason;

import java.io.File;

/**
 * Created by ruedi on 02.03.17.
 */
public class ApiTest extends Actor<ApiTest> {

    public IPromise<String> hello(String name) {
        System.out.println("hello received:"+name);
        return resolve("Hello "+name);
    }

    public void cyclicPing(Callback cb) {
        cb.stream("Hello "+System.currentTimeMillis());
        delayed(1000,() -> cyclicPing(cb) );
    }

    public void load() {
    }

    public static void main(String[] args) {
        ApiTest wsTest = Actors.AsActor(ApiTest.class);
        HttpPublisher pub = new HttpPublisher(wsTest,"localhost","api",7777)
            .coding(new Coding(SerializerType.JsonNoRef, new Class[] {DenialReason.class} ));
        pub.publish( act -> {
                System.out.println("DISCON");
            }, new TestConstraints()
        );
        Http4K.get().publishFileSystem("localhost","html",7777,new File("/home/ruedi/projects/mnistplay/src/main/script/"));
        Http4K.get().publishFileSystem("localhost","scripts",7777,new File("/home/ruedi/projects/kontraktor/modules/kontraktor-http/src/main/javascript/js4k/"));

    }
}
