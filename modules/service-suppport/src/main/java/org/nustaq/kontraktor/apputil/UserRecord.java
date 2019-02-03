package org.nustaq.kontraktor.apputil;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

// key is registration email
public class UserRecord extends RecordWrapper {

    public UserRecord(Record record) {
        super(record);
    }

    public UserRecord(String key) {
        super(key);
    }

    public UserRecord name(final String name) {
        put("name", name );
        return this;
    }

    public UserRecord email(final String email) {
        put("email", email );
        return this;
    }

    public UserRecord type(final String type) {
        put("type", type );
        return this;
    }

    public UserRecord pwd(final String pwd) {
        put("pwd", pwd );
        return this;
    }

    public UserRecord creation(final long creation) {
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

    public String getPwd() {
        return getString("pwd" );
    }

    public long getCreation() {
        return getLong("creation");
    }

}
