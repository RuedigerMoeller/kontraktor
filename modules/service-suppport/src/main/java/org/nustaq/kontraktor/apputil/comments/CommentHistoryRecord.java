package org.nustaq.kontraktor.apputil.comments;

import org.nustaq.kontraktor.apputil.recordwrappermixins.CreationMixin;
import org.nustaq.kontraktor.apputil.recordwrappermixins.ForeignKeyMixin;
import org.nustaq.kontraktor.apputil.recordwrappermixins.IdMixin;
import org.nustaq.kontraktor.apputil.recordwrappermixins.TypeMixin;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.Set;

public class CommentHistoryRecord extends RecordWrapper implements
    CreationMixin<CommentHistoryRecord>,
    TypeMixin<CommentHistoryRecord>, // edit, add, del
    ForeignKeyMixin<CommentHistoryRecord>, // points to commentTree
    IdMixin<CommentHistoryRecord> // points to commentTree's subcomment
{
    public CommentHistoryRecord(Record record) {
        super(record);
    }

    public CommentHistoryRecord(String key) {
        super(key);
    }

    public Set<String> getMentions() {
        return (Set<String>) get("mentions");
    }

    public CommentHistoryRecord mentions(Set<String> m) {
        put("mentions",m);
        return this;
    }

    public String getAffectedParentUser() {
        return getString("affectedParentUser");
    }

    // reference by user name
    public CommentHistoryRecord affectedParentUser(String m) {
        put("affectedParentUser",m);
        return this;
    }

    public String getEditorId() {
        return getString("editorId");
    }

    public CommentHistoryRecord editorId(String m) {
        put("editorId",m);
        return this;
    }
}
