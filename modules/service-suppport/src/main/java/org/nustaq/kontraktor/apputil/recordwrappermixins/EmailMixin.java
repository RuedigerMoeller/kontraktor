package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface EmailMixin<T extends RecordWrapper> extends Record {

    default String getEmail() {
        return getString("email");
    }

    default T email(final String text) {
        this.put("email", text);
        return (T) this;
    }

}
