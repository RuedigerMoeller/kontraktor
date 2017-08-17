package reactinstrinsic.servlet;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Scheduler;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.impl.SimpleScheduler;

import java.util.Random;
import java.util.stream.IntStream;

/**
 * minimal implementation of session based server (incl. load balancing)
 */
public class ReactServletApp extends Actor<ReactServletApp> {

    private Scheduler clientThreads[];
    private Random rand = new Random();

    @Local
    public void init(int nthreads) {
        clientThreads = new Scheduler[nthreads];
        IntStream.range(0,nthreads)
            .forEach( i -> clientThreads[i] = new SimpleScheduler(100, true /*Important!*/ ));
    }

    public IPromise<ReactServletSession> login(String username) {
        if ( "".equals(username.trim()) ) {
            return reject("empty username");
        }
        ReactServletSession session = AsActor(
            ReactServletSession.class,
            // randomly distribute session actors among clientThreads
            clientThreads[rand.nextInt(clientThreads.length)]
        );
        session.init(username);
        return resolve(session); // == new Promise(session)
    }

}
