package org.nustaq.http.example;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;

/**
 * Created by ruedi on 20.06.17.
 */
public class ServletSession extends Actor<ServletSession> {

    ServletApp app;
    String user;

    public void init(ServletApp app, String user) {
        this.app = app;
        this.user = user;
    }

    public IPromise<String> whatsYourName() {
        System.out.println("whatsYourName " + user);
        return new Promise<>("Rüdiger "+user);
    }

}
