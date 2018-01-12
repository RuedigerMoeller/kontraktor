package org.nustaq.kontraktor.apputil;

import java.util.Date;
import java.util.Properties;
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

    static Mailer singleton;

    public static void initSingleton(MailCfg settings) {
        Mailer m = AsActor(Mailer.class);
        m.init(settings);
        singleton = m;
    }

    public static Mailer get() {
        return singleton;
    }

    MailCfg settings;

    public void init( MailCfg conf ) {
        updateSettings(conf);
    }

    public void updateSettings( MailCfg conf ) {
        this.settings = conf;
    }

    /**
     *
     * @param receiver - the mail receiver
     * @param subject - subject of the mail
     * @param content - mail content
     * @param displayName - display name shown instead of the sender email ..
     * @return promise ..
     */
    public IPromise<Boolean> sendMail( String receiver, String subject, String content, String displayName /* Sender Name*/ ) {
        if (receiver == null || !receiver.contains("@")){
            return new Promise<>(false, "Not a valid email address: " + receiver);
        }
        try {
            Properties props = new Properties();
            props.put("mail.smtp.auth", settings.getSmtpAuth());
            props.put("mail.smtp.starttls.enable", settings.getStartTls());
            props.put("mail.smtp.host", settings.getSmtpHost());
            props.put("mail.smtp.port", settings.getSmtpPort());

            Session session = Session.getInstance(props);
            MimeMessage message = new MimeMessage(session);

            message.setFrom( displayName == null ?  new InternetAddress("support@juptr.io") : new InternetAddress("support@juptr.io", displayName)  );
            message.setSubject(subject);
            message.setText(content,"utf-8", "html");
            message.setRecipient(Message.RecipientType.TO, new InternetAddress(receiver, false));
            message.setSentDate(new Date());
            Transport.send(message, settings.getUser(), settings.getPassword());
            Log.Info(this, "definitely sent mail to "+receiver+" subject:" + subject );
            return new Promise<>(true);
        } catch (Exception e) {
            Log.Warn(this, e);
            return new Promise<>(false,e);
        }
    }

}
