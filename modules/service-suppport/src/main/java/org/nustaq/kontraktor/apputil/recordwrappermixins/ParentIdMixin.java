package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface ParentIdMixin<T extends RecordWrapper> extends Record {

    default String getParentId() {
        return getString("parentId");
    }

    default T parentId(final String creation) {
        this.put("parentId", creation);
        return (T) this;
    }

}
