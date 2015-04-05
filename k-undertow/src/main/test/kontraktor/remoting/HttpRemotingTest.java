package kontraktor.remoting;

import junit.framework.Assert;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Register;
import org.nustaq.kontraktor.remoting.http.RestActorClient;
import org.nustaq.kontraktor.remoting.http.RestActorServer;
import org.nustaq.kontraktor.undertow.KUndertowHttpServerAdapter;
import org.nustaq.kontraktor.undertow.Knode;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by ruedi on 04.04.2015.
 */
public class HttpRemotingTest {

    public static class Pojo {
        String name;
        int id;
        Set<String> follower = new HashSet<>();

        public Pojo() { // required for json !!
        }

        public Pojo(String name, int id, Set<String> follower) {
            this.name = name;
            this.id = id;
            this.follower = follower;
        }

        public String getName() {
            return name;
        }

        public int getId() {
            return id;
        }

        public Set<String> getFollower() {
            return follower;
        }
    }

    @Register(HttpRemotingTest.Pojo.class)
    public static class HttpTestService extends Actor<HttpTestService> {

        //http://localhost:8080/api/test/$test/hello
        public void $test(String s) {
            System.out.println(s);
        }

        //http://localhost:8080/api/test/$hello/1/2/3/4/5/'Guten%20Tag%20!'
        public void $hello( byte b, short s, int i, long l, char c, String str ) {
            System.out.println("byte "+b+", short "+s+", int "+i+", long "+l+", char "+c+", String "+str);
        }

        //http://localhost:8080/api/test/$helloBig/1/2/3/4/5/'Guten%20Tag%20!'
        public void $helloBig( Byte b, Short s, Integer i, Long l, Character c, String str ) {
            System.out.println("byte "+b+", short "+s+", int "+i+", long "+l+", char "+c+", String "+str);
        }
        //http://localhost:8080/api/test/$callback/
        public void $callback( Callback cb ) {
            delayed( 500, () -> cb.stream("A") );
            delayed( 1000, () -> cb.stream("B") );
            delayed( 1500, () -> cb.stream("C") );
            delayed( 2000, () -> cb.finish() );
        }

        public IPromise $promise(String s) {
            return new Promise(s+" "+s);
        }

        public IPromise<Pojo> $clonePojo(Pojo pojo) {
            return new Promise<>(pojo);
        }

    }

    @Test
    public void startServer() throws InterruptedException {
        int port = 8080;
        Knode knode = new Knode();
        knode.mainStub(new String[] {"-p",""+port});
        KUndertowHttpServerAdapter sAdapt = new KUndertowHttpServerAdapter(
            knode.getServer(),
            knode.getPathHandler()
        );

        RestActorServer restActorServer = new RestActorServer();
        restActorServer.joinServer("/api", sAdapt);

        HttpTestService service = Actors.AsActor(HttpTestService.class);
        restActorServer.publish("test",service);

        RestActorClient<HttpTestService> cl = new RestActorClient<>("localhost",8080,"/api/test/",HttpTestService.class);
        cl.connect();
        HttpTestService clientProxy = cl.getFacadeProxy();

        Set<String> set = new HashSet<>();
        set.add("PAK");
        Pojo p = new Pojo("POK",13,set);
        Pojo p1 = clientProxy.$clonePojo(p).await();
        Assert.assertTrue(p.getName().equals(p1.getName()) && p.getFollower().size() == p1.getFollower().size());


        Assert.assertTrue(clientProxy.$promise("hello").await().equals("hello hello"));
        clientProxy.$callback( (r,e) -> {
            System.out.println(r+" - "+e);
        });

        Thread.sleep(500000);
    }
}
