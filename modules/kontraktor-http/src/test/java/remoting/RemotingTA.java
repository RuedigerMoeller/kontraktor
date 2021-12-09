package remoting;

import com.eclipsesource.json.JsonObject;
import com.eclipsesource.json.WriterConfig;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.RateLimited;
import org.nustaq.kontraktor.remoting.base.JsonMapped;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.undertow.HttpPublisher;

public class RemotingTA extends Actor<RemotingTA> {

    public IPromise<Integer> sayHello(int count, Callback<String> cb) {
        delayed(100, () -> {
            for ( int i=0; i<count; i++ )
                cb.pipe(i+" 'th String");
            cb.finish();
        });
        return resolve(count);
    }

    @RateLimited(callsPerMinute = 5)
    public IPromise yes() {
        return resolve("yes");
    }

    public @JsonMapped IPromise<SampleDTO> testJsonArgMapped(@JsonMapped SampleDTO dto) {
        return resolve(dto);
    }

    public IPromise<SampleDTOMarked> testMarkedJsonArg(SampleDTOMarked dto) {
        return resolve(dto);
    }

    public IPromise<JsonObject> testJsonObjectArg(JsonObject dto) {
        return resolve(dto);
    }

    public static void main(String[] args) {
        RemotingTA serv = Actors.AsActor(RemotingTA.class);
        new HttpPublisher(serv,"0.0.0.0","/httpapi",7779)
            .serType(SerializerType.JsonNoRef)
            .publish()
            .await();
    }
}
