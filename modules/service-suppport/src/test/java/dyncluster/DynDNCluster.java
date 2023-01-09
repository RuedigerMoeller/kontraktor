package dyncluster;

import org.nustaq.kontraktor.services.datacluster.DBClient;
import org.nustaq.reallive.api.RealLiveTable;
import org.nustaq.reallive.api.Record;

public class DynDNCluster {

    public static void main(String[] args) {
        MyServiceRegistry.main(new String[0]);

        int nodes = 3;
        for (int i = 0; i < nodes; i++) {
            SoaDynDataShard.main(new String[] {""+i});
        }

        DBClient dbClient = new DBClient();
        dbClient.connect(new DBClient.DBClientArgs());
        RealLiveTable testTbl = dbClient.getDClient().tbl("test");
        for (int i = 0; i < 5000; i++) {
            testTbl.join(Record.from(
                "key",""+i,
                "value", "13-"+i
            ));
        }
    }

}
