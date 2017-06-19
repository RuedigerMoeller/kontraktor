package org.nustaq.http.example;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;

import java.util.Date;

/**
 * Created by ruedi on 19.06.17.
 */
public class ServletApp extends Actor<ServletApp> {

    public void init() {
    }

    public IPromise<String> hello(String s) {
        System.out.println("hello received "+s);
        return resolve(s+" "+new Date());
    }

}
