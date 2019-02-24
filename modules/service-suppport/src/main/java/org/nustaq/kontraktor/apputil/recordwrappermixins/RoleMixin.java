package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface RoleMixin<T extends RecordWrapper> extends Record {

    default String getRole() {
        return getString("role");
    }

    default T role(final String text) {
        this.put("role", text);
        return (T) this;
    }

}
