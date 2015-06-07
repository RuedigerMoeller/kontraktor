package reallive;

import org.junit.Test;
import org.nustaq.reallive.RecordChange;
import org.nustaq.reallive.impl.storage.TestRec;

/**
 * Created by ruedi on 21.06.14.
 */
public class ChangeTracking {


    @Test
    public void testChange() {
        TestRec org = new TestRec("pok", null);

        TestRec toChange = new TestRec(org);

        toChange.setName("otherName");
        toChange.setArr(new int[]{99,99});
        toChange.setX(-14);

        RecordChange recordChange = toChange.computeDiff();

        TestRec applied = new TestRec(org);
        System.out.println("before: "+applied);
        recordChange.apply(applied);
        System.out.println("after: " + applied);

        System.out.println(recordChange);
    }

}
