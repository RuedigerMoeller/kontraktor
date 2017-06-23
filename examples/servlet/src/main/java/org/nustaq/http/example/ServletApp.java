package org.nustaq.http.example;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

import java.util.Date;

/**
 * Created by ruedi on 19.06.17.
 */
public class ServletApp extends BasicWebAppActor<ServletApp,BasicWebAppConfig> {

    public IPromise<String> hello(String s) {
        System.out.println("hello received "+s);
        return resolve(s+" "+new Date());
    }

    @Override
    protected IPromise<String> verifyCredentials(String user, String pw, String jwt)  {
        if ( "admin".equals(user)) {
            return reject("authentication failed");
        }
        return resolve("logged in");
    }

    @Override
    protected Class getSessionClazz() {
        return ServletSession.class;
    }
}
