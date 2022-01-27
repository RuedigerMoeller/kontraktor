package dyncluster;

import org.nustaq.kontraktor.services.datacluster.DataShardArgs;
import org.nustaq.kontraktor.services.datacluster.dynamic.DynDataShard;

public class SoaDynDataShard {

    public static void main(String[] args) {
        DataShardArgs dargs = DataShardArgs.New();
        dargs.shardNo( Integer.parseInt(args[0]) );
        DynDataShard.start(dargs);
    }
}
