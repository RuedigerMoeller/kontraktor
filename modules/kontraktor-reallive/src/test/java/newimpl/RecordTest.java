package newimpl;

import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.nustaq.reallive.messages.ChangeUtils;
import org.nustaq.reallive.messages.Diff;

import org.nustaq.reallive.api.Record;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.assertTrue;

/**
 * Created by ruedi on 04.08.2015.
 */
public class RecordTest {

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
        Assert.assertFalse(diff.isEmpty());
    }

    @Test
    public void testOmit() {
        Record omit = Record.from(
            "key", "test",
            "a", Record.from("b", 10, "pwd", "erutz0w9rw0e9r8"),
            "pwd", "wpeoriwe8rw9er8w9r"
        ).omit("pwd");
        Assert.assertNull(omit.get("pwd"));
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
        Assert.assertNull(test.get("a+"));
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
        Assert.assertEquals(2, rr.getArr("z").length);
        Assert.assertEquals(r, cpy);
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
        Assert.assertFalse(r.containsKey("x"));
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
        Assert.assertEquals(r, cpy);
    }

    @Test
    public void testPutNullIdempotency() {
        final String testKey = "test";
        final Record record = Record.from(
                "key", "1",
                testKey, "testValue1"
        );

        assertEquals(1, record.getFields().length);
        assertEquals(testKey, record.getFields()[0]);

        record.put(testKey, null);
        assertEquals(0, record.getFields().length);

        record.put(testKey, null);
        assertEquals(0, record.getFields().length);
    }

    @Test
    public void testOmitRecursivelyInPlace() {
        final String testKey = "testKey";
        final Record record = Record.from(
                "key", "1",
                testKey, "testValue1",
                "subRecord1", Record.from(
                        "key", "2",
                        testKey, "testValue2",
                        "subRecord2", Record.from(
                                "key", "3",
                                testKey, "testValue3"
                        )
                )
        );

        assertEquals(3, StringUtils.countMatches(record.toPrettyString(), testKey));
        record.omitRecursivelyInPlace(testKey);
        assertEquals(0, StringUtils.countMatches(record.toPrettyString(), testKey));
        record.omitRecursivelyInPlace(testKey);
        assertEquals(0, StringUtils.countMatches(record.toPrettyString(), testKey));
    }
}
