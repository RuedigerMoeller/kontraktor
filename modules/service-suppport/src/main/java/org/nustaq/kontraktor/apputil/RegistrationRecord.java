package org.nustaq.kontraktor.apputil;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;
import org.nustaq.serialization.coders.Unknown;

import java.util.UUID;

public class RegistrationRecord extends RecordWrapper {
    public static final String TYPE_REGISTRATION = "Registration";

//    String name;
//    String email;
//    boolean confirmed;
//    String pwd;

    public RegistrationRecord(Record record) {
        super(record);
    }

    public RegistrationRecord(String key) {
        super(key);
        type(TYPE_REGISTRATION);
    }

    public RegistrationRecord(Unknown data) {
        this(UUID.randomUUID().toString());
        data.getFields().forEach( (k,v) -> put(k,v));
    }

    public RegistrationRecord name(final String name) {
        put("name", name );
        return this;
    }

    public RegistrationRecord email(final String email) {
        put("email", email );
        return this;
    }

    public RegistrationRecord confirmed(final boolean confirmed) {
        put("confirmed", confirmed );
        return this;
    }

    public RegistrationRecord type(final String type) {
        put("type", type );
        return this;
    }

    public RegistrationRecord pwd(final String pwd) {
        put("pwd", pwd );
        return this;
    }

    public RegistrationRecord creation(final long creation) {
        put("creation", creation );
        return this;
    }
    public String getName() {
        return getString("name" );
    }

    public String getType() {
        return getString("type" );
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

    public long getCreation() {
        return getLong("creation");
    }
}
