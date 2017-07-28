package org.nustaq.kontraktor.weblication;

import java.io.Serializable;

/**
 * Created by ruedi on 07.07.17.
 */
public class BasicAuthenticationResult implements Serializable {

    protected String userKey;
    protected Object initialData; // set after loadSession

    public String getUserKey() {
        return userKey;
    }

    public BasicAuthenticationResult userName(String userName) {
        this.userKey = userName;
        return this;
    }

    /**
     * contains result of loadSession (and is filled just after that)
     * @return
     */
    public Object getInitialData() {
        return initialData;
    }

    public BasicAuthenticationResult initialData(final Object sessionInitResult) {
        this.initialData = sessionInitResult;
        return this;
    }


}
