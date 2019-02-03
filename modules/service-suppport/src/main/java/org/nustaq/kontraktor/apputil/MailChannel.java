package org.nustaq.kontraktor.apputil;

import java.io.Serializable;

public class MailChannel implements Serializable {
    public MailChannel(String symbolicName, String email, String displayName) {
        this.symbolicName = symbolicName;
        this.email = email;
        this.displayName = displayName;
    }

    String symbolicName;
    String email;
    String displayName;

    public String getSymbolicName() {
        return symbolicName;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
