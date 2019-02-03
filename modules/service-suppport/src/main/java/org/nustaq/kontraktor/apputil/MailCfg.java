package org.nustaq.kontraktor.apputil;

import org.nustaq.kson.Kson;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by ruedi on 02/09/15.
 */
public class MailCfg implements Serializable {

    public static MailCfg read( String file ) throws Exception {
        MailCfg mailCfg = (MailCfg) new Kson().map(MailCfg.class, MailChannel.class).readObject(new File(file), MailCfg.class);
        mailCfg.initChannels();
        return mailCfg;
    }

    private void initChannels() {
        channelMap = new HashMap<>();
        for (int i = 0; i < channelConf.length; i++) {
            MailChannel mailChannel = channelConf[i];
            channelMap.put(mailChannel.getSymbolicName(),mailChannel);
        }
        if ( channelMap.get("default") == null )
        {
            MailChannel def = new MailChannel("default", "system@system.io", "System");
            channelMap.put(def.getSymbolicName(),def);
        }
    }

    String user = "none";
    String password = "none";
    String smtpHost = "none";
    String smtpPort = "none";
    String smtpAuth = "none";
    String startTls = "none";
    String templateBase = "./run/templates/";

    transient Map<String,MailChannel> channelMap;

    MailChannel channelConf[] = {
        new MailChannel( "default","system@system.io","System"),
        new MailChannel( "registration","registration@system.io","System Registration"),
    };

    public String getTemplateBase() {
        return templateBase;
    }

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
    public MailChannel getChannel( String name ) {
        if ( channelMap == null )
            initChannels();
        MailChannel mailChannel = channelMap.get(name);
        if ( mailChannel == null )
            return channelMap.get("default");
        return mailChannel;
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
