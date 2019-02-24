package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface AuthorMixin<T extends RecordWrapper> extends Record {

    default String getAuthor() {
        return getString("author");
    }

    default T author(final String text) {
        this.put("author", text);
        return (T) this;
    }

}
