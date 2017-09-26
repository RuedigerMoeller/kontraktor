package org.nustaq.kontraktor.weblication;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public class UserRecord extends RecordWrapper {

    protected UserRecord(Record record) {
        super(record);
    }

    public UserRecord(String key) {
        super(key);
    }

    public String getNick() {
        return getKey();
    }

    public String getPwd() {
        return getString("pwd");
    }

    public boolean isVerified() {
        return getBool("verified");
    }

    public UserRecord verified(boolean b) {
        put("verified", b);
        return this;
    }

}
