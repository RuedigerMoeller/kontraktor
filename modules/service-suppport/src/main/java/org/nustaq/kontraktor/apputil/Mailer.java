package org.nustaq.kontraktor.apputil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.BiFunction;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.Log;

import javax.mail.Message;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

/**
 * Created by ruedi on 02/09/15.
 *
 * This helper actor can be used to send mails.
 *
 * Configuration is part of JuptrConfig
 *
 */
public class Mailer extends Actor<Mailer> {

    public static boolean DEBUG_MAIL = false;

    static Mailer singleton;

    public static void initSingleton(MailCfg settings, String publicUrl) {
        Mailer m = AsActor(Mailer.class);
        m.init(settings,publicUrl);
        singleton = m;
    }

    public static Mailer get() {
        return singleton;
    }

    MailCfg settings;
    String publicUrl;

    public void init(MailCfg conf, String publicUrl) {
        this.publicUrl = publicUrl;
        updateSettings(conf);
    }

    public void updateSettings( MailCfg conf ) {
        this.settings = conf;
    }

    public static String applyTemplate(String templateFileRelativeToTemplateDir, Map<String,Object> data, BiFunction<String,Object,String> mapFun) throws IOException {
        String t = new String(Files.readAllBytes(Paths.get(Mailer.get().getActor().settings.getTemplateBase()+templateFileRelativeToTemplateDir)), "UTF-8");
        for (Map.Entry<String, Object> e : data.entrySet()) {
            t = t.replace( "$"+e.getKey()+"$", mapFun.apply(e.getKey(),e.getValue()));
        }
        return t;
    }

    /**
     * @param receiver    - the mail receiver
     * @param subject     - subject of the mail
     * @param content     - mail content
     * @param senderEmail - email adress from sender
     * @param displayName - display name shown instead of the sender email ..
     * @return promise ..
     */
    public IPromise<Boolean> sendEMail(String receiver, String subject, String content, String senderEmail, String displayName /* Sender Name*/) {
        if (receiver == null || !receiver.contains("@")) {
            return new Promise<>(false, "Not a valid email address: " + receiver);
        }
        if (DEBUG_MAIL) {
            System.out.println("EMAIL to:"+receiver+" "+subject+" from:"+senderEmail+" "+displayName);
            System.out.println(content);
            return new Promise<>(true);
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", settings.getSmtpAuth());
            props.put("mail.smtp.starttls.enable", settings.getStartTls());
            props.put("mail.smtp.host", settings.getSmtpHost());
            props.put("mail.smtp.port", settings.getSmtpPort());

            Session session = Session.getInstance(props);
            MimeMessage message = new MimeMessage(session);

            message.setFrom(displayName == null ? new InternetAddress(senderEmail) : new InternetAddress(senderEmail, displayName));
            message.setSubject(subject);
            message.setText(content, "utf-8", "html");
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(receiver, false));
            message.setSentDate(new Date());
            Transport.send(message, settings.getUser(), settings.getPassword());
            Log.Info(this, "definitely sent mail to " + receiver + " subject:" + subject);
            return new Promise<>(true);
        } catch (Exception e) {
            Log.Warn(this, e);
            return new Promise<>(false, e);
        }
    }

    /**
     *
     * @param receiver - the mail receiver
     * @param subject - subject of the mail
     * @param content - mail content
     * @return promise ..
     */
    public IPromise<Boolean> sendDefaultMail( String receiver, String subject, String content ) {
        return sendChannelMail("default", receiver,subject,content);
    }

    public IPromise<Boolean> sendChannelMail( String channel, String receiver, String subject, String content ) {
        MailChannel channelSettings = settings.getChannel(channel);
        return sendEMail(receiver,subject,content,channelSettings.getEmail(),channelSettings.getDisplayName());
    }

    public IPromise<Boolean> sendTemplateChannelMail( String channel, String receiver, String subject, String templateFile, Map<String,Object> data ) {
        MailChannel channelSettings = settings.getChannel(channel);
        data.put("public-url",publicUrl);
        try {
            String content = applyTemplate(templateFile, data, (k, v) -> "" + v);
            return sendEMail(receiver,subject,content,channelSettings.getEmail(),channelSettings.getDisplayName());
        } catch (IOException e) {
            Log.Error(this,e);
            return reject(e);
        }
    }

}
