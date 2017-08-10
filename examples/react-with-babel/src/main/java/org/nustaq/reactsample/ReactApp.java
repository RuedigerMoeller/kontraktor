package org.nustaq.reactsample;

import org.nustaq.kontraktor.annotations.Remoted;
import org.nustaq.kontraktor.annotations.Secured;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.weblication.BasicWebAppActor;
import org.nustaq.reallive.records.MapRecord;


/**
 * Created by ruedi on 30.05.17. This is the singleton Actor representing your App.
 * Single Instance Main Application Actor
 *
 * remote calls on the main App can be done without authentication, so take care
 */
@Secured // enforce explicit tagging of remoteable methods (@Remote) effects ALSO Session actors
public class ReactApp extends BasicWebAppActor<ReactApp,ReactAppConfig> {

    @Override
    public void init(ReactAppConfig cfg) {
        super.init(cfg);
        // fill in some predefined accounts from our config
        String[] initialUsers = cfg.getInitialUsers();
        if ( initialUsers != null ) {
            for (int i = 0; i < initialUsers.length; i+=3) {
                sessionStorage.putUserIfNotPresent(
                    MapRecord.New(initialUsers[i])
                        .put("pwd",initialUsers[i+1])
                        .put("text",initialUsers[i+2])
                        .put("verified",true)
                );
            }
        }
    }

    @Override
    protected Class getSessionClazz() {
        return ReactAppSession.class;
    }

    @Remoted
    public IPromise register(String nick, String pwd, String text) {
        Promise p = new Promise();
        sessionStorage.putUserIfNotPresent(
            MapRecord.New(nick)
                .put("pwd",pwd)
                .put("text",text)
                .put("verified",true)
        ).then( (r,e) -> {
            if ( ! r ) {
                p.reject("user "+nick+" already exists");
            } else {
                p.resolve(true);
            }
        });
        return p;
    }

    @Override
    protected IPromise<String> getDirectRequestResponse(String path) {
        String[] split = path.split("/");
        if ( split.length == 0 )
            return resolve("<html>Invalid Link</html>");
        Promise res = new Promise();
        sessionStorage.takeToken( split[split.length-1], false).then( (token,err) -> {
            if ( token != null ) {
                res.resolve("<html>User confirmed: '"+token.getUserId()+"' data:"+token.getData()+"</html>");
                // lets increment a count for that
                // note: not atomic
                sessionStorage.getUser(token.getUserId()).then( record -> {
                    // increment
                    record.put("count",record.getInt("count")+1);
                    // save
                    sessionStorage.putUser(record);
                    // session cached record is stale now => update
                    // this is a bad example, should use events instead and ofc
                    // default session storage is not a database replacement
                    sessions.values().stream()
                        .filter( sess -> sess.getUserKey().equals(token.getUserId()))
                        .forEach(sess -> ((ReactAppSession)sess).updateUserRecord(record));
                });
            } else
                res.reject("<html>Expired or invalid Link</html>");
        });
        return res;
    }

}
