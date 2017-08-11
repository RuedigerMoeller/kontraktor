package sample.httpjs;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.remoting.base.RemotedActor;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by ruedi on 30/05/15.
 *
 * per client session state+api obtained if a client successfully logged in
 *
 */
public class MyHttpAppSession extends Actor<MyHttpAppSession> implements RemotedActor {

    MyHttpApp app;
    ArrayList<String> toDo = new ArrayList<>();
    Callback subscription;
    Pojo aPojo;

    public void init(MyHttpApp app, List<String> todo) {
        Thread.currentThread().setName("Session Dispatcher");
        this.app = app;
        aPojo = new Pojo("pojo");
        aPojo.addPojo(new Pojo("otherPojo"));
        aPojo.addPojo(new Pojo("yetAnother"));
        toDo.addAll(todo);
        pushEventLoop();
    }

    public IPromise<ArrayList<String>> getToDo() {
        return resolve(toDo);
    }

    public IPromise<Pojo> getPojo() {
        return new Promise<>(aPojo);
    }

    public IPromise<Pojo> pojoRoundTrip(Pojo pojo) {
        return new Promise<>(pojo);
    }

    public void streamToDo( String filter, Callback cb ) {
        toDo.forEach( item -> {
            if ( item.indexOf(filter) >= 0 ) {
                cb.pipe(item);
            }
        });
        cb.finish();
    }

    public void subscribe( Callback cb ) {
        subscription = cb;
    }

    public void unsubscribe() {
        if ( subscription != null ) {
            subscription.finish();
            subscription = null;
        }
    }

    public IPromise<String> test1(String username, String password, List<Address> addresses, String device1, String device2, String residentialStatus,
        int pval, String employStatus, int employDuration, int grossIncome, int usersIncome, String bank ) {
        return resolve("1");
    }

    public void pushEventLoop() {
        if ( ! isStopped() ) {
            if ( subscription != null ) {
                app.getNumSessions().then( sessions -> {
                    subscription.pipe(new Date().toString()+", "+sessions+" Session Actors");
                });
            }
            delayed( 2000, () -> pushEventLoop() );
        }
    }

    @Override
    public void hasBeenUnpublished() {
        app.clientClosed(self());
        self().stop();
    }
}
