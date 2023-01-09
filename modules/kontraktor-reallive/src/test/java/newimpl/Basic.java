package newimpl;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.Promise;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.reallive.messages.ChangeUtils;
import org.nustaq.reallive.messages.Diff;
import org.nustaq.reallive.records.PatchingRecord;
import org.nustaq.reallive.server.actors.RealLiveTableActor;
import org.nustaq.reallive.client.ShardedTable;
import org.nustaq.reallive.server.actors.TableSpaceActor;
import org.nustaq.reallive.api.*;
import org.nustaq.reallive.server.*;
import org.nustaq.reallive.server.storage.*;
import org.nustaq.reallive.records.MapRecord;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;
import org.nustaq.reallive.api.Record;

/**
 * Created by ruedi on 04.08.2015.
 */
public class Basic {

    @Test public void diffing() {
        Record from = Record.from(
            "key", "aKey",
            "test", "13",
            "arr", new Object[] { 1,2,3,new Object[] {4,5,6} },
            "sub", Record.from("a", "12", "b", "13")
        );
        Record test1 = Record.from(
            "key", "aKey",
            "test", "13",
            "sub", Record.from("a", "12", "b", "14")
        );
        Diff diff = ChangeUtils.computeDiff(test1,from);
        Assert.assertTrue(!diff.isEmpty());
    }

    @Test
    public void testOmit() {
        Record omit = Record.from(
            "key", "test",
            "a", Record.from("b", 10, "pwd", "erutz0w9rw0e9r8"),
            "pwd", "wpeoriwe8rw9er8w9r"
        ).omit("pwd");
        Assert.assertTrue(omit.get("pwd") == null );
    }

    @Test
    public void testStripOps() {
        Record test = Record.from(
            "key", "test",
            "a+", Record.from("b", 10, "pwd", "erutz0w9rw0e9r8"),
            "pwd?+", "wpeoriwe8rw9er8w9r"
        );
        test.stripOps();
        System.out.println(test.toPrettyString());
        Assert.assertTrue(test.get("a+") == null );
    }

    @Test
    public void testNullHandling() {
        Record r = Record.from(
            "x", null,
            "y",1,
            "z", new Object[] { 1,"y",3, "y" }
        );
        Record cpy = r.deepCopy();
        System.out.println(r.toPrettyString());
        System.out.println(cpy.toPrettyString());
        Record rr = r.transformCopy((k, i, v) -> ("y".equals(k) || "y".equals(v)) ? null : v);
        System.out.println(rr.toPrettyString());
        Assert.assertTrue(cpy.containsKey("x"));
        Assert.assertTrue(cpy.get("x") == Record._NULL_ );
        Assert.assertTrue(rr.getArr("z").length == 2 );
        Assert.assertTrue(r.equals(cpy));
    }

    @Test
    public void testJoinNullHandling() {
        Record r = Record.from(
            "x", 1,
            "y",1
        );
        Record toJoin = Record.from(
            "x", null
        );
        r.join(toJoin);
        Assert.assertTrue(!r.containsKey("x"));
        System.out.println(r.toPrettyString());
    }

    @Test
    public void testTransformCopy() {
        Record r = Record.from(
            "x", 13,
            "sub", Record.from(
                "x", 99,
                "xx", new Object[] { 1,3,"four",77},
                "yy", 15.0
            )
        );
        Record cpy = r.deepCopy();
        Record transformCpy = r.transformCopy( (k,i,v) -> {
            if ( "x".equals(k) ) {
                return 100_000;
            } else if ( i == 0 ) {
                return Record.from( "77", new Object[]{"seventyseven"}, "x", 10 );
            }
            return v;
        });
        System.out.println(r.toPrettyString());
        System.out.println(transformCpy.toPrettyString());
        Assert.assertTrue(r.equals(cpy));
    }
}
