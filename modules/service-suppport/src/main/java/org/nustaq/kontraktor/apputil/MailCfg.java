package org.nustaq.kontraktor.apputil;

import java.io.Serializable;
/**
 * Created by ruedi on 02/09/15.
 */
public class MailCfg implements Serializable {

    String user = "none";
    String password = "none";
    String smtpHost = "none";
    String smtpPort = "none";
    String smtpAuth = "none";
    String startTls = "none";

    public String getUser() {
        return user;
    }
    public String getPassword() {
        return password;
    }
    public String getSmtpHost() {
        return smtpHost;
    }
    public String getSmtpPort() {
        return smtpPort;
    }
    public String getSmtpAuth() {
        return smtpAuth;
    }
    public String getStartTls() {
        return startTls;
    }

    public MailCfg user(String user) {
        this.user = user;
        return this;
    }

    public MailCfg password(String password) {
        this.password = password;
        return this;
    }

    public MailCfg smtpHost(String smtpHost) {
        this.smtpHost = smtpHost;
        return this;
    }

    public MailCfg smtpPort(String smtpPort) {
        this.smtpPort = smtpPort;
        return this;
    }

    public MailCfg smtpAuth(String smtpAuth) {
        this.smtpAuth = smtpAuth;
        return this;
    }

    public MailCfg startTls(String startTls) {
        this.startTls = startTls;
        return this;
    }
}
