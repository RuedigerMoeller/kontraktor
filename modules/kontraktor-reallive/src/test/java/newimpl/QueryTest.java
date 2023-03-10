package newimpl;

import org.junit.Assert;
import org.junit.Test;
import org.nustaq.reallive.api.Record;
import org.nustaq.reallive.query.CompiledQuery;
import org.nustaq.reallive.query.Parser;
import org.nustaq.reallive.query.Query;
import org.nustaq.reallive.query.Value;
import org.nustaq.reallive.records.MapRecord;

import java.util.UUID;

public class QueryTest {

    @Test
    public void testNested() {
        CompiledQuery nums = Query.compile("exists( externalAccounts.timerbee.id )");
        Record r = Record.from(
            "externalAccounts", Record.from(
                "timerbee", Record.from("id", "13") )
            );
        Assert.assertTrue(nums.evaluate(r).isTrue());
        Record r1 = Record.from(
            "externalAccounts", Record.from(
                "timerbee", Record.from("id", null) )
        );
        Assert.assertFalse(nums.evaluate(r1).isTrue());
        Record r2 = Record.from(
            "externalAccounts", Record.from(
                "timerbee", null )
        );
        Assert.assertFalse(nums.evaluate(r2).isTrue());
        Record r3 = Record.from(
            "externalAccounts", Record.from(
                "timerbee", Record.from("id", "") )
        );
        Assert.assertFalse(nums.evaluate(r3).isTrue());
        Record r4 = Record.from(
            "externalAccounts", Record.from(
                "timerbee", Record.from("id", false) )
        );
        Assert.assertTrue(nums.evaluate(r4).isTrue());
        Record r5 = Record.from(
            "externalAccounts", Record.from(
                "timerbee", Record.from("id", 0) )
        );
        Assert.assertFalse(nums.evaluate(r5).isTrue());
        Record r6 = Record.from(
            "externalAccounts", Record.from(
                "timerbee", Record.from("id", 1) )
        );
        Assert.assertTrue(nums.evaluate(r6).isTrue());
    }

    @Test
    public void testEquals() {
        String uuid = UUID.randomUUID().toString();
        Record r = Record.from(
            "key", uuid
        );
        CompiledQuery nums = Query.compile("_key == '"+uuid+"'");
        Value evaluate = nums.evaluate(r);
        Assert.assertTrue(evaluate.isTrue());
    }

}
