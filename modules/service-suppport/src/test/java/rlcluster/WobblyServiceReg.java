package rlcluster;

import org.nustaq.kontraktor.util.Log;

public class WobblyServiceReg extends TestCluster
{
    public static void main(String[] args) throws Exception {
        checkDir();
        startRegistry();
        Thread.sleep(30000);
        Log.Info(TestRegistryfailure.class,"termiante registry");
        System.exit(1000);
    }
}
