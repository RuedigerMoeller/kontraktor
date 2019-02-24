package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.apputil.recordwrappermixins.*;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;
import org.nustaq.serialization.coders.Unknown;

import java.util.UUID;

public class RegistrationRecord extends RecordWrapper implements
    CreationMixin<RegistrationRecord>,
    TypeMixin<RegistrationRecord>,
    NameMixin<RegistrationRecord>,
    EmailMixin<RegistrationRecord>,
    PwdMixin<RegistrationRecord>
{
    public static final String TYPE_REGISTRATION = "Registration";

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

    public RegistrationRecord confirmed(final boolean confirmed) {
        put("confirmed", confirmed );
        return this;
    }

    public boolean isConfirmed() {
        return getBool("confirmed" );
    }

}
