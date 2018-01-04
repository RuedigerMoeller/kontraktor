package org.nustaq.kontraktor.services.rlclient;

import com.beust.jcommander.Parameter;
import org.nustaq.kontraktor.services.ServiceArgs;

/**
 * Created by ruedi on 15.08.2015.
 */
public class DataShardArgs extends ServiceArgs {

    @Parameter( required = true, names = { "-sn","-shardNo" })
    int shardNo;

    public int getShardNo() {
        return shardNo;
    }

    @Override
    public String toString() {
        return "DataShardArgs{" +
                   "shardNo=" + shardNo +
                   "} " + super.toString();
    }
}
