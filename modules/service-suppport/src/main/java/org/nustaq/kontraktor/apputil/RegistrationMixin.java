package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.remoting.http.undertow.builder.BldFourK;
import org.nustaq.kontraktor.util.Log;
import org.nustaq.reallive.api.Record;
import org.nustaq.serialization.coders.Unknown;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public interface RegistrationMixin<SELF extends Actor<SELF>> extends LinkMapperMixin<SELF> {

    static String UserTableName = "user";

    long MAX_AGE = TimeUnit.DAYS.toMillis(3);

    static void auto(BldFourK bld, Object registrationMixin) {
        LinkMapperMixin.auto(bld,registrationMixin);
    }

    /**
     * UI entry point
     * @param data
     * @return
     */
    default IPromise<RegistrationRecord> register(Unknown data) {
        Promise p = new Promise();
        RegistrationRecord registerform = new RegistrationRecord(data);
        registerform.creation(System.currentTimeMillis());
        putRecord(registerform).then( (r,e) -> {
            if ( r != null ) {
                HashMap<String, Object> copiedMap = createData(data, registerform);
                Mailer.get().sendTemplateChannelMail(
                    "registration",
                    registerform.getEmail(),
                    "Bitte bestätigen sie Ihre Registrierung",
                    "mail/opt-in.html",
                    copiedMap);
                p.resolve(registerform);
            } else {
                p.reject(e);
            }
        });
        return p;
    }

    static HashMap<String, Object> createData(Unknown data, RegistrationRecord registerform) {
        HashMap<String,Object> copiedMap = new HashMap<>();
        data.getFields().forEach( (k,v) -> copiedMap.put(k,v));
        copiedMap.put("confirm-link", LinkTableName +"/"+registerform.getKey());
        return copiedMap;
    }

    @CallerSideMethod @Local
    default String handleLinkSuccess(String linkId, Record linkRecord) {
        if ( linkRecord.getSafeString("type").equals("Registration") ) {
            RegistrationRecord rec = new RegistrationRecord(linkRecord);
            if ( System.currentTimeMillis() - rec.getCreation() > MAX_AGE ) {
                // outdated
                return applyTemplate(rec, "html/registration-old.html");
            } else {
                //TODO: create user, check uniqueness
                return applyTemplate(rec, "html/registration-success.html");
            }
        } else { // wrong type
            return applyTemplate(null, "html/registration-fail.html");
        }
    }

    static String applyTemplate(RegistrationRecord rec, String template) {
        try {
            Map<String, Object> data = rec != null ? rec.asMap() : new HashMap<>();
            return Mailer.applyTemplate(template, data,(k, v) -> ""+v);
        } catch (IOException e) {
            Log.Warn(RegistrationMixin.class,e);
            return "<html>internal error "+e+"</html>";
        }
    }

    @CallerSideMethod @Local
    default String handleLinkFailure(String linkId) {
        return null;
    }

}
