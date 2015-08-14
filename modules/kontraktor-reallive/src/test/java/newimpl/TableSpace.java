package newimpl;

import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.reallive.impl.tablespace.TableSpaceActor;
import org.nustaq.reallive.interfaces.Mutation;
import org.nustaq.reallive.interfaces.RealLiveTable;
import org.nustaq.reallive.interfaces.TableDescription;

/**
 * Created by ruedi on 14.08.2015.
 */
public class TableSpace {

    @Test
    public void simple() {

        TableSpaceActor ts = Actors.AsActor(TableSpaceActor.class);

        ts.init(4, 0);

        if ( ts.getTable("Test").await() == null ) {
            TableDescription test =
                new TableDescription("Test")
                    .filePath("/tmp/test")
                    .numEntries(500_000)
                    .sizeMB(500)
                    .shardNo(0);
            ts.createTable(test).await();
        }
        RealLiveTable test = ts.getTable("Test").await();
        Mutation mutation = test.getMutation();
        mutation.add("emil", "age", 9, "name", "emil");

        ts.shutDown().await();
    }
}
