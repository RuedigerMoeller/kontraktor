package org.nustaq.kontraktor.apputil.comments;
import org.nustaq.kontraktor.apputil.recordwrappermixins.*;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.records.RecordWrapper;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by ruedi on 28/06/16.
 *
 * A full tree of CommentRecords for a single article
 *
 * key: did#articleId
 *
 */
public class CommentRecord extends RecordWrapper implements
    CreationMixin<CommentRecord>,
    LastModifiedMixin<CommentRecord>,
    IdMixin<CommentRecord>,
    AuthorMixin<CommentRecord>,
    ImageUrlMixin<CommentRecord>,
    TextMixin<CommentRecord>,
    RoleMixin<CommentRecord>
{

    public CommentRecord(Record record) {
        super(record);
    }

    public CommentRecord(String key) {
        super(key);
    }

    public int getUp() {
        return getInt("up");
    }

    public int getDown() {
        return getInt("down");
    }

    public CommentRecord up(final int up) {
        this.put("up", up);
        return this;
    }

    public CommentRecord down(final int down) {
        this.put("down", down);
        return this;
    }

    public CommentRecord children(final List<Record> children) {
        this.put("children", children.stream().map( r -> (r instanceof RecordWrapper) ? ((RecordWrapper)r).getRecord() : r ).collect(Collectors.toList()) );
        return this;
    }

    public void addChild( CommentRecord cRecord ) {
        List crecs = getChildCommentRecords();
        crecs.add(0,cRecord);
        children(crecs);
    }

    public List<Record> getChildren() {
        Object children = get("children");
        if ( children == null )
            return new ArrayList<>();
        return (List<Record>) children;
    }

    public List<CommentRecord> getChildCommentRecords() {
        return getChildren().stream().map( r -> new CommentRecord(r) ).collect(Collectors.toList());
    }

    public CommentRecord findChildNode(String nodeId) {
        if ( getId().equals(nodeId) )
            return this;
        List<Record> children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            CommentRecord rec = new CommentRecord(children.get(i));
            CommentRecord childNode = rec.findChildNode(nodeId);
            if ( childNode != null )
                return childNode;
        }
        return null;
    }

    public CommentRecord delChildNode(String commentId) {
        List<Record> children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            CommentRecord rec = new CommentRecord(children.get(i));
            if ( rec.getId().equalsIgnoreCase(commentId) ) {
                children.remove(i);
                return rec;
            }
            CommentRecord commentRecord = rec.delChildNode(commentId);
            if ( commentRecord != null )
                return commentRecord;
        }
        return null;
    }

    public CommentRecord findParent(String childId) {
        List<Record> children = getChildren();
        for (int i = 0; i < children.size(); i++) {
            CommentRecord rec = new CommentRecord(children.get(i));
            if ( rec.getId().equals(childId) )
                return this;
            rec = rec.findParent(childId);
            if ( rec != null )
                return rec;
        }
        return null;
    }

    public static int getNumCommenters(List<Record> children) {
        if ( children == null )
            return 0;
        return (int)children.stream().map( rec -> rec.getString("author").toLowerCase() ).distinct().count();
    }

}
