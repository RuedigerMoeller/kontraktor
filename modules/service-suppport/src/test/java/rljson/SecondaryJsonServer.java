package rljson;

import org.nustaq.kontraktor.impl.BackOffStrategy;
import org.nustaq.kontraktor.services.rlserver.RLJsonServer;
import org.nustaq.kontraktor.services.rlserver.SimpleRLConfig;

public class SecondaryJsonServer extends RLJsonServer {

    public static void main(String[] args) throws InterruptedException {
        SimpleRLConfig.pathname = "./etc/reallive1.kson";
        BackOffStrategy.SLEEP_NANOS = 5 * 1000 * 1000; // 5 millis
        Class<SecondaryJsonServer> appClazz = SecondaryJsonServer.class;
        startUp(args, appClazz);
    }


}
