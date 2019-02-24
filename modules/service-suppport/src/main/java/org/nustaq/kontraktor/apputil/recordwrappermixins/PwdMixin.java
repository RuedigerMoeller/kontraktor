package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface PwdMixin<T extends RecordWrapper> extends Record {

    default String getPwd() {
        return getString("pwd");
    }

    default T pwd(final String text) {
        this.put("pwd", text);
        return (T) this;
    }

}
