package org.nustaq.kontraktor.apputil;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public class RegistrationRecordWrapper extends RecordWrapper {

//    String name;
//    String email;
//    boolean confirmed;
//    String pwd;

    public RegistrationRecordWrapper(Record record) {
        super(record);
    }

    public RegistrationRecordWrapper(String key) {
        super(key);
    }

    public RegistrationRecordWrapper name(final String name) {
        put("name", name );
        return this;
    }

    public RegistrationRecordWrapper email(final String email) {
        put("email", email );
        return this;
    }

    public RegistrationRecordWrapper confirmed(final boolean confirmed) {
        put("confirmed", confirmed );
        return this;
    }

    public RegistrationRecordWrapper pwd(final String pwd) {
        put("pwd", pwd );
        return this;
    }

    public String getName() {
        return getString("name" );
    }

    public String getEmail() {
        return getString("email" );
    }

    public boolean isConfirmed() {
        return getBool("confirmed" );
    }

    public String getPwd() {
        return getString("pwd" );
    }
}
