package kontraktor.remoting;

import io.undertow.server.handlers.resource.ResourceHandler;
import org.junit.Test;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.javascript.DependencyResolver;
import org.nustaq.kontraktor.remoting.spa.AppConf;
import org.nustaq.kontraktor.undertow.Knode;
import myapp.SPAServer;
import org.nustaq.kontraktor.undertow.javascript.DynamicResourceManager;
import org.nustaq.serialization.FSTConfiguration;

import java.util.Map;
import java.util.function.Consumer;

/**
 * Created by ruedi on 07/04/15.
 */
public class SPATest {


    @Test
    public void run() throws InterruptedException {
        Knode knode = new Knode();
        knode.mainStub(new String[] {"-p",""+8080});

        SPAServer mySpa = Actors.AsActor(SPAServer.class);

        String appRootDirectory = "/home/ruedi/projects/kontraktor/k-undertow/src/main/test/myapp/";
        String appRootPath = "myapp/";

        Map<String,DependencyResolver> loader = mySpa.$main(appRootDirectory).await();
        AppConf conf = mySpa.$getConf().await();


        knode.publishOnHttp(appRootPath+"http/", "api", mySpa);
        Consumer<FSTConfiguration> configurator = mySpa.$getRemotingConfigurator().await();
        knode.publishOnWebsocket(appRootPath+"ws/", SerializerType.MinBin, mySpa, configurator, true);

        loader.forEach( (relPath, deploader) -> {
            DynamicResourceManager man = new DynamicResourceManager(true, deploader.getRootComponent(), deploader);
            knode.getPathHandler().addPrefixPath(appRootPath, new ResourceHandler(man));
        });


        Thread.sleep(1000000);
        knode.stop();
    }

}
