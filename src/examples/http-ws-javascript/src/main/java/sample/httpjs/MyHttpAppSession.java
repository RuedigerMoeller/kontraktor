package sample.httpjs;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.RemoteActorInterface;
import org.nustaq.kontraktor.remoting.base.RemotedActor;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ruedi on 30/05/15.
 */
public class MyHttpAppSession extends Actor<MyHttpAppSession> implements RemotedActor {

    MyHttpApp app;
    ArrayList<String> toDo = new ArrayList<>();

    public void init(MyHttpApp app, List<String> todo) {
        this.app = app;
        toDo.addAll(todo);
    }

    public IPromise<ArrayList<String>> getToDo() {
        return resolve(toDo);
    }

    @Override
    public void $hasBeenUnpublished() {
        app.clientClosed();
        self().$stop();
    }
}
