package cmd;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.services.datacluster.DBClient;
import org.nustaq.kontraktor.util.Log;

import java.util.concurrent.TimeUnit;

public class DBSelect extends DBClient {

    public static class DBSelectArgs extends DBClient.DBClientArgs {
        @Parameter(names={"-t"}, description ="table to query", required = true)
        String tableName;

        @Parameter(names={"-q"}, description ="query string", required = true )
        String query;
    }

    protected DBClientArgs createArgs() {
        return new DBSelectArgs();
    }

    protected void executeCode() {
        DBSelectArgs args = (DBSelectArgs) this.args;
        long now = System.currentTimeMillis();
        for (int i = 0; i < 1; i++) {
            dclient.tbl(args.tableName).query(args.query, (r,e) -> {
                if ( r != null )
                    System.out.println(r.toPrettyString());
                else {
                    System.out.println("done in "+(System.currentTimeMillis()-now));
                    exit(0);
                }
            });
        }
    }

    public static void main(String[] args) {
        Log.setLevel(Log.ERROR);
        DBClient dbClient = new DBSelect();
        dbClient.connectAndStart(args, dbClient);
    }

}
