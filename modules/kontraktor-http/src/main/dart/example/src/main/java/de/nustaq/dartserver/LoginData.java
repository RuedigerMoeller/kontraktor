package de.nustaq.dartserver;

import org.nustaq.kontraktor.Actor;

import java.io.Serializable;
import java.util.Map;

public class LoginData implements Serializable {

    Actor session;
    Map<String,Object> user; // userrecord

    public LoginData session(final Actor session) {
        this.session = session;
        return this;
    }

    public LoginData user(final Map<String,Object> user) {
        this.user = user;
        return this;
    }

    public Actor getSession() {
        return session;
    }

    public Map<String,Object> getUser() {
        return user;
    }
}
