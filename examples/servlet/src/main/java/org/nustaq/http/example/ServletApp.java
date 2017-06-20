package org.nustaq.http.example;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;

import java.util.Date;

/**
 * Created by ruedi on 19.06.17.
 */
public class ServletApp extends Actor<ServletApp> {

    private Scheduler[] sessionThreads;

    public void init() {
        sessionThreads = new Scheduler[]{
            new SimpleScheduler(10_000,true),
            new SimpleScheduler(10_000,true),
        };
    }

    public IPromise<String> hello(String s) {
        System.out.println("hello received "+s);
        return resolve(s+" "+new Date());
    }

    public void push(Callback<String> cb) {
        if ( ! isStopped() ) {
            cb.stream(""+new Date());
            delayed(1000, () -> push(cb) );
        }
    }

    public IPromise<ServletSession> login(String user, String pw) {
        //TODO: verify user, pw ASYNC !
        if ( "admin".equals(user)) {
            return new Promise<>(null,"hehe");
        }
        ServletSession sess = Actors.AsActor(ServletSession.class,sessionThreads[(int) (Math.random()*sessionThreads.length)]);
        sess.init(self(),user);
        return new Promise<>(sess);
    }

}
