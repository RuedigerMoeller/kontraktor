package remoting;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

public class RemotingTA extends Actor<RemotingTA> {

    public IPromise<Integer> sayHello(int count, Callback<String> cb) {
        delayed(100, () -> {
            for ( int i=0; i<count; i++ )
                cb.pipe(i+" 'th String");
            cb.finish();
        });
        return resolve(count);
    }

}
