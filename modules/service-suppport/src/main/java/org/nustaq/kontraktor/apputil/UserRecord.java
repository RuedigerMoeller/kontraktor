package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.apputil.recordwrappermixins.*;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

// key is registration email
public class UserRecord extends RecordWrapper implements
    NameMixin<UserRecord>,
    CreationMixin<UserRecord>,
    EmailMixin<UserRecord>,
    PwdMixin<UserRecord>,
    TypeMixin<UserRecord>,
    RoleMixin<UserRecord>,
    ImageUrlMixin<UserRecord>
{

    public UserRecord(Record record) {
        super(record);
    }

    public UserRecord(String key) {
        super(key);
    }

    public static UserRecord lightVersion(Record rec) {
        UserRecord urec = new UserRecord(rec);
        return new UserRecord(urec.getName())
            .name(urec.getName())
            .imageURL(urec.getImageURL())
            .role(urec.getRole());
    }
}
