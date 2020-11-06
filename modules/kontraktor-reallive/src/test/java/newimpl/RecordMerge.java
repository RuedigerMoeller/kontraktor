package newimpl;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.reallive.api.Record;

public class RecordMerge {

    @Test
    public void testBaseMerge() {
        Record rec = Record.from(
            "key", "xy13key",
            "bl", true
        );
        rec.deepMerge(Record.from(
            "xy", "test"
        ));
        Assert.assertTrue(rec.get("xy").equals("test"));
        rec.deepMerge(Record.from(
            "xy", null
        ));
        Assert.assertTrue(rec.get("xy") == null);
    }

    @Test
    public void testRecordMerge() {
        Record rec = Record.from(
            "key", "xy13key",
            "sub", Record.from(
                "x", 1,
                "y", 2
            )
        );

        rec.deepMerge(Record.from(
            "sub+", Record.from( "test", 13 )
        ));
        Assert.assertTrue( rec.mget("sub", "test").equals(13) && rec.mget("sub", "x").equals(1) );

        rec.deepMerge(Record.from(
            "sub", Record.from( "test", 13 )
        ));
        Assert.assertTrue( rec.mget("sub", "test").equals(13) && rec.mget("sub", "x") == null );

        rec.deepMerge(Record.from(
            "sub", "test"
        ));
        Assert.assertTrue( "test".equals( rec.get("sub") ) );
    }

}
