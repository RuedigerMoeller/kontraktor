package org.nustaq.http.example;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.impl.SimpleScheduler;
import java.util.Date;
import java.util.Random;
import java.util.stream.IntStream;

/**
 * Created by ruedi on 19.06.17.
 */
public class ServletApp extends Actor<ServletApp> {

    private Scheduler clientThreads[];
    private Random rand = new Random();

    public void init(int nthreads) {
        clientThreads = new Scheduler[nthreads];
        IntStream.range(0,nthreads)
            .forEach( i -> clientThreads[i] = new SimpleScheduler(100, true /*Important!*/ ));
    }

    public IPromise<String> hello(String s) {
        System.out.println("hello received "+s);
        return resolve(s+" "+new Date());
    }

    public IPromise login(String user, String pwd) {
        if ( !"admin".equals(user) ) {
            ServletSession servletSession =
                AsActor(
                    ServletSession.class,
                    clientThreads[rand.nextInt(clientThreads.length)]
                );
            servletSession.init(user);
            return new Promise(new Object[] {servletSession,"some initial data for SPA"});
        }
        return reject("access denied");
    }
}
