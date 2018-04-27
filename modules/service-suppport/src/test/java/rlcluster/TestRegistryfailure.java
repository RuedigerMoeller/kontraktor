package rlcluster;

import org.nustaq.kontraktor.services.ServiceRegistry;
import org.nustaq.kontraktor.services.rlclient.DataShard;
import org.nustaq.kontraktor.util.Log;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;

public class TestRegistryfailure extends TestCluster {

    public static void main(String[] args) throws Exception {
        checkDir();

        initDataNodes();

    }
}
