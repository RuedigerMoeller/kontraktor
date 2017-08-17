package org.nustaq.intrinsicreact;

import org.nustaq.kontraktor.weblication.BasicWebAppConfig;

public class ReactAppConfig extends BasicWebAppConfig {

    public static ReactAppConfig read() {
        return (ReactAppConfig) BasicWebAppConfig.read(ReactAppConfig.class);
    }

    String initialUsers[] = {"username","password","text"};

    public String[] getInitialUsers() {
        return initialUsers;
    }

    public ReactAppConfig initialUsers(String[] initialUsers) {
        this.initialUsers = initialUsers;
        return this;
    }
}
