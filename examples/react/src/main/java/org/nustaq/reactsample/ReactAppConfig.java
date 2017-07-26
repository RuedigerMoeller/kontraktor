package org.nustaq.reactsample;

import org.nustaq.kontraktor.weblication.BasicWebAppConfig;
public class ReactAppConfig extends BasicWebAppConfig {

    public static ReactAppConfig read() {
        return (ReactAppConfig) BasicWebAppConfig.read(ReactAppConfig.class);
    }

    String initialUsers[] = {"username","password"};

    public String[] getInitialUsers() {
        return initialUsers;
    }

}
