package org.nustaq.kontraktor.apputil.recordwrappermixins;

import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

public interface ForeignKeyMixin<T extends RecordWrapper> extends Record {

    default String getForeignKey() {
        return getString("foreignKey");
    }

    default T foreignKey(final String creation) {
        this.put("foreignKey", creation);
        return (T) this;
    }

}
