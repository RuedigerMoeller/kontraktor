package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.Actor;
import org.nustaq.reallive.api.Record;

import java.io.Serializable;

public class LoginData implements Serializable {

    Actor session;
    Record user; // userrecord

    public LoginData session(final Actor session) {
        this.session = session;
        return this;
    }

    public LoginData user(final UserRecord user) {
        this.user = user.getRecord();
        return this;
    }

    public Actor getSession() {
        return session;
    }

    public UserRecord getUser() {
        return new UserRecord(user);
    }
}
