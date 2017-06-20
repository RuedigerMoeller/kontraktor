package org.nustaq.kontraktor.application;

import org.nustaq.kontraktor.Actor;

/**
 * Created by ruedi on 20.06.17.
 */
public class BasicWebAppSession<T extends BasicWebAppSession> extends Actor<T> {

    protected BasicWebApp app;
    protected String user;
    /**
     * this can be null in case of websocket connections FIXME
     */
    protected String sessionId;

    public void init(BasicWebApp app, String user, String sessionId) {
        this.app = app;
        this.user = user;
        this.sessionId = sessionId;
    }

}
