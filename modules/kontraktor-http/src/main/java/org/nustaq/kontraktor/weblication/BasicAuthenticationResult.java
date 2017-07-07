package org.nustaq.kontraktor.weblication;

import java.io.Serializable;

/**
 * Created by ruedi on 07.07.17.
 */
public class BasicAuthenticationResult implements Serializable {

    protected String userName;

    public String getUserName() {
        return userName;
    }

    public BasicAuthenticationResult userName(String userName) {
        this.userName = userName;
        return this;
    }
}
