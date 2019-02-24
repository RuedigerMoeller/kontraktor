package org.nustaq.kontraktor.apputil.comments;

import com.google.common.collect.Tables;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;
import org.nustaq.kontraktor.apputil.RegistrationMixin;
import org.nustaq.kontraktor.apputil.UserRecord;
import org.nustaq.kontraktor.services.rlclient.DataClient;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;

import java.util.*;

import static org.nustaq.kontraktor.Actors.resolve;

/**
 * supports
 * * commenting a whole tree of comments
 * * a change history table
 * * mentions @username (link is genereated to profile like /#/profiles/[username]
 * * some user refs are by user name, so user name must be equal (cannot use email key as they should be private)
 */
public interface CommentsSessionMixin {

    String TableName = "comment";
    String HistoryTableName = "commentHistory";
    String DefaultUserImagePath = "./imgupload/default-user.png";

    @CallerSideMethod @Local
    DataClient getDClient();

    @CallerSideMethod @Local
    UserRecord getUser();

    default IPromise<Record> createDiscussion() {
        return getOrCreateDiscussion(UUID.randomUUID().toString());
    }

    default IPromise<Record> getOrCreateDiscussion(String commentTreeKey) {
        Promise res = new Promise();
        getDClient().tbl(TableName).get(commentTreeKey).then( (r,e) -> {
            if ( r != null )
                res.resolve(r);
            else {
                long now = System.currentTimeMillis();
                CommentRecord rec =
                    new CommentRecord(commentTreeKey)
                        .author(getUser().getName())
                        .creation(now)
                        .lastModified(now)
                        .imageURL(getUser().getImageURL())
                        .text("root")
                        .id("root");
                getDClient().tbl(TableName).setRecord(rec.getRecord());
                res.resolve(rec.getRecord());
            }
        });
        return res;
    }

    default IPromise<CommentRecord> getParentComment(String commentTreeKey, String subCommentId ) {
        if ( commentTreeKey == null )
            return resolve(null);
        RealLiveTable comments = getDClient().tbl(TableName);
        return comments.atomic( commentTreeKey, res -> {
            if ( res != null ) {
                CommentRecord comment = new CommentRecord(res);
                return comment.findParent(subCommentId);
            } else
                return null;
        });
    }

    default void delComment( String commentKey, String commentId ) {
        RealLiveTable comments = getDClient().tbl(TableName);
        String editorKey = getUser().getKey();
        comments.atomic( commentKey, ctree -> {
            if ( ctree == null ) {
            } else {
                CommentRecord root = new CommentRecord(ctree);
                if ( root.getId() == null )
                    root.id("root");
                CommentRecord childNode = root.findChildNode(commentId);
                if ( childNode != null ) {
                    childNode.text("[deleted]");
                    root.children(root.getChildren()); // trigger change
                    CommentRecord parent = root.findParent(commentId);
                    CommentHistoryRecord ch =
                        new CommentHistoryRecord(UUID.randomUUID().toString())
                            .foreignKey(commentKey).id(commentId)
                            .creation(System.currentTimeMillis())
                            .affectedParentUser(parent.getAuthor())
                            .editorId(editorKey)
                            .type("del");
                    return childNode;
                } else {
                    // FIXME: error handling
                }
            }
            return null;
        }).then( (ch,e) -> {
            if ( ch != null ) {
                CommentHistoryRecord casted = (CommentHistoryRecord) ch;
                getDClient().tbl(HistoryTableName).addRecord(casted);
            }
        });
    }

    default IPromise editComment( String commentTreeKey, String commentId, String text0 ) {
        Promise res = new Promise();
        Set<String> mentions = new HashSet();
        String editorKey = getUser().getKey();
        RealLiveTable comments = getDClient().tbl(TableName);
        highLighComment(text0,0,mentions).then( text -> {
            comments.atomic(commentTreeKey, ctree -> {
                if (ctree == null) {
                } else {
                    CommentRecord root = new CommentRecord((Record) ctree);
                    if (root.getId() == null)
                        root.id("root");
                    CommentRecord childNode = root.findChildNode(commentId);
                    if (childNode != null) {
                        long now = System.currentTimeMillis();
                        childNode.text(text);
                        childNode.lastModified(now);
                        root.lastModified(now);
                        root.children(root.getChildren());
                        CommentRecord parent = root.findParent(commentId);
                        CommentHistoryRecord ch =
                            new CommentHistoryRecord(UUID.randomUUID().toString())
                                .foreignKey(commentTreeKey).id(commentId)
                                .creation(now)
                                .editorId(editorKey)
                                .affectedParentUser(parent != null ? parent.getAuthor() : null)
                                .type("edit");
                        return ch;
                    } else {
                        // FIXME: error handling
                    }
                }
                return null;
            }).then(ch -> {
                if (ch != null) {
                    CommentHistoryRecord casted = (CommentHistoryRecord) ch;
                    casted.mentions(mentions);
                    getDClient().tbl(HistoryTableName).addRecord(casted);
                    res.resolve(commentId);
                } else {
                    res.reject("error: no comment record could be found");
                }
            });
            return;
        });
        return res;
    }

    default IPromise addComment( String commentTreeKey, String parentCommentId, String text0 ) {
        Promise res = new Promise();
        Set<String> mentions = new HashSet();
        RealLiveTable comments = getDClient().tbl(TableName);
        String ukey = getUser().getKey();
        String uImg = getUser().getImageURL();
        highLighComment(text0,0,mentions).then( text -> {
            CommentRecord newComment = new CommentRecord("")
                .id(UUID.randomUUID().toString())
                .creation(System.currentTimeMillis())
                .lastModified(System.currentTimeMillis())
                .text(text)
                .author(getUser().getName())
                .imageURL(uImg);
            if ( newComment.getImageURL() == null || newComment.getImageURL().length() == 0) {
                newComment.imageURL(DefaultUserImagePath);
            }
            comments.atomic( commentTreeKey, ctree -> {
                if ( ctree == null ) {
                } else {
                    CommentRecord root = new CommentRecord(ctree);
                    if ( root.getId() == null )
                        root.id("root");
                    CommentRecord parentNode = root.findChildNode(parentCommentId);
                    if ( parentNode != null ) {
                        parentNode.addChild(newComment);
                        root.children(root.getChildren());
                        CommentHistoryRecord ch =
                            new CommentHistoryRecord(UUID.randomUUID().toString())
                                .foreignKey(commentTreeKey).id(parentCommentId)
                                .creation(System.currentTimeMillis())
                                .editorId(ukey)
                                .affectedParentUser(parentNode.getAuthor())
                                .type("add");
                        return ch;
                    } else {
                        // FIXME: error handling
                    }
                }
                return null;
            }).then(ch -> {
                if (ch != null) {
                    CommentHistoryRecord casted = (CommentHistoryRecord) ch;
                    casted.mentions(mentions);
                    getDClient().tbl(HistoryTableName).addRecord(casted);
                }
            });
            res.resolve(newComment.getRecord());
        });
        return res;
    }

    // scan for mentions and replace them with links
    default IPromise<String> highLighComment(String comment, int index, Set mentions) {
        Promise res = new Promise();
        if ( comment != null || index >= comment.length()) {
            int idx = comment.indexOf("@",index);
            if ( idx > 0 ) {
                char c = comment.charAt(idx-1);
                if ( c != 32 && c != 160 && c != '>' && c != ';' ) {
                    return highLighComment(comment,idx+1,mentions);
                }
            }
            RealLiveTable userTable = getDClient().tbl(RegistrationMixin.UserTableName);
            while( idx >= 0 ) {
                if ( idx >= 0 ) {
                    int idx1 = idx+1;
                    boolean quoted = false;
                    if ( idx1 < comment.length() && comment.charAt(idx1) == '\'' ) { // name with spaces @'Rüdiger Möller'
                        idx1++;
                        idx++;
                        while( idx1 < comment.length() && comment.charAt(idx1) != '\'' ) {
                            idx1++;
                        }
                        quoted = true;
                    } else {
                        while (idx1 < comment.length() && Character.isLetterOrDigit(comment.charAt(idx1))) {
                            idx1++;
                        }
                    }
                    if ( idx1 > idx ) {
                        String name = comment.substring(idx+1,idx1);
                        int finalIdx = idx1+(quoted ? 1 : 0);
                        int finalIdx1 = idx;
                        userTable.find(rec -> rec.getSafeString("name").equalsIgnoreCase(name) )
                            .then( (rec,err) -> {
                                if ( rec != null ) {
                                    UserRecord ulight = UserRecord.lightVersion(rec);
                                    String unam = ulight.getName();
                                    String replace = "<a href='/#/profiles/" + unam + "'>" + unam + "</a>";
                                    String formatted = comment.substring(0, finalIdx1) + replace + comment.substring(finalIdx);
                                    mentions.add(rec.getKey());
                                    highLighComment(formatted, finalIdx1 +replace.length(),mentions).then(res);
                                }
                        });
                        return res;
                    }
                }
            }
        }
        return resolve(comment);
    }

}
