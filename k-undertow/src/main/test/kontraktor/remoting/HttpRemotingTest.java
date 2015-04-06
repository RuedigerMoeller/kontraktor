package kontraktor.remoting;

import junit.framework.Assert;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.junit.Test;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.Register;
import org.nustaq.kontraktor.remoting.http.HttpObjectSocket;
import org.nustaq.kontraktor.remoting.http.RestActorClient;
import org.nustaq.kontraktor.remoting.http.RestActorServer;
import org.nustaq.kontraktor.undertow.KUndertowHttpServerAdapter;
import org.nustaq.kontraktor.undertow.Knode;
import org.nustaq.kontraktor.util.PromiseLatch;
import org.nustaq.kontraktor.util.RateMeasure;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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

    static AtomicInteger callCount = new AtomicInteger(0);

    @Register(HttpRemotingTest.Pojo.class)
    public static class HttpTestService extends Actor<HttpTestService> {

        //http://localhost:8080/api/test/$test/hello
        public void $test(String s) {
            System.out.println(s);
            callCount.incrementAndGet();
        }

        public void $bench(String s) {
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
        public void $callback( String dummy, Callback cb ) {
            callCount.incrementAndGet();
            delayed( 500, () -> cb.stream("A") );
            delayed( 1000, () -> cb.stream("B") );
            delayed( 1500, () -> cb.stream("C") );
            delayed( 2000, () -> cb.finish() );
        }

        public IPromise $promise(String s) {
            callCount.incrementAndGet();
            return new Promise(s+" "+s);
        }

        public IPromise<Pojo> $clonePojo(Pojo pojo) {
            callCount.incrementAndGet();
            return new Promise<>(pojo);
        }

    }

    @Test
    public void startServer() throws InterruptedException {
        AtomicInteger successCount = new AtomicInteger(0);
        int port = 8080;
        Knode knode = new Knode();
        knode.mainStub(new String[] {"-p",""+port});
        KUndertowHttpServerAdapter sAdapt = new KUndertowHttpServerAdapter(
            knode.getServer(),
            knode.getPathHandler()
        );

        HttpObjectSocket.DUMP_REQUESTS = false;
        RestActorServer restActorServer = new RestActorServer();
        restActorServer.joinServer("/api", sAdapt);

        HttpTestService service = Actors.AsActor(HttpTestService.class);
        restActorServer.publish("test",service);

        RestActorClient<HttpTestService> cl = new RestActorClient<>("localhost",8080,"/api/test/",HttpTestService.class);
        cl.connect();
        HttpTestService clientProxy = cl.getFacadeProxy();

        clientProxy.$test("Hello");

        Set<String> set = new HashSet<>();
        set.add("PAK");
        Pojo p = new Pojo("POK",13,set);
        Pojo p1 = clientProxy.$clonePojo(p).await();
        Assert.assertTrue(p.getName().equals(p1.getName()) && p.getFollower().size() == p1.getFollower().size());


        Assert.assertTrue(clientProxy.$promise("hello").await().equals("hello hello"));
        clientProxy.$callback( "Bla", (r,e) -> {
            System.out.println(r+" - "+e);
            successCount.incrementAndGet();
        });


        ///////////// VOID message /////////////////////////////////////////////////////////

        // use plain http post + kson
        try {
            String request = "[ { method: '$test' args: [ 'Hello' ] } ]";
            request = URLEncoder.encode(request);

            Content content = Request.Post("http://localhost:8080/api/test")
                .addHeader("Accept", "text/kson")
                .bodyString(request, ContentType.create("text/kson"))
                .execute()
                .returnContent();
            System.out.println("Result:");
            System.out.println(content.asString());
            successCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

        // use plain http post + json
        try {
            String request = "[ { 'method': '$test', 'args': [ 'Hello' ] } ]";
            request = URLEncoder.encode(request);

            Content content = Request.Post("http://localhost:8080/api/test")
                .bodyString(request, ContentType.create("text/json"))
                .execute()
                .returnContent();
            System.out.println("Result:");
            System.out.println(content.asString());
            successCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

        ///////////// Callback message /////////////////////////////////////////////////////////

        // use plain http post + kson
        try {
            String request =
                "[ " +
                    "{ method: '$callback' args: [ 'bla', rcb { cbid: 1 } ] } " +
                    "{ method: '$callback' args: [ 'bla', rcb { cbid: 2 } ] } " +
                "]";
            request = URLEncoder.encode(request);

            Content content = Request.Post("http://localhost:8080/api/test")
                .addHeader("Accept", "text/kson")
                .bodyString(request, ContentType.create("text/kson"))
                .execute()
                .returnContent();
            System.out.println("Result:");
            System.out.println(content.asString());
            successCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

        // use plain http post + json
        try {
            String request = "[ { method: '$callback', args: [ 'bla', { '_type' : 'rcb', 'cbid' : 1 } ] } ]";
            request = URLEncoder.encode(request);

            Content content = Request.Post("http://localhost:8080/api/test")
                .bodyString(request, ContentType.create("text/json"))
                .execute()
                .returnContent();
            System.out.println("Result:");
            System.out.println(content.asString());
            successCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

        ///////////// promise + Pojo /////////////////////////////////////////////////////////

        // use plain http post + kson
        try {
            String request =
            "[ " +
                "{ " +
                    "futureKey: 1 "+
                    "method: '$clonePojo' args: [ " +
                        "Pojo { name: 'bla' id: 14 follower: [ X , Y , 'Z Z' ] }" +
                    "] " +
                "} " +
            "]";
            request = URLEncoder.encode(request);

            Content content = Request.Post("http://localhost:8080/api/test")
                .addHeader("Accept", "text/kson")
                .bodyString(request, ContentType.create("text/kson"))
                .execute()
                .returnContent();
            System.out.println("Result:");
            System.out.println(content.asString());
            successCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

        // use plain http post + json
        try {
            String request =
                "[" +
                    "{ " +
                        "futureKey: 1 , " +
                        "method: '$clonePojo', " +
                        "args: [" +
                            " { " +
                                "'_type' : 'Pojo', " +
                                "'name' : 'name', " +
                                "'follower': " +
                                    "[ 'a' , 'b' ]" +
                            " } " +
                        "] " +
                    "}" +
                "]";
            request = URLEncoder.encode(request);

            Content content = Request.Post("http://localhost:8080/api/test")
                .bodyString(request, ContentType.create("text/json"))
                .execute()
                .returnContent();
            System.out.println("Result:");
            System.out.println(content.asString());
            successCount.incrementAndGet();
        } catch (IOException e) {
            e.printStackTrace();
            Assert.assertTrue(false);
        }

        RateMeasure measure = new RateMeasure("plain void req");
        for (int i = 0; i < 200_000; i++) {
            clientProxy.$bench(null);
            measure.count();
        }

        clientProxy.$ping().await();
        System.out.println("start future bench");

        RateMeasure pmeasure = new RateMeasure("promise req");
        Promise finished = new Promise();
        PromiseLatch latch = new PromiseLatch(50_000,finished);
        for (int i = 0; i < 50_000; i++) {
            clientProxy.$promise("").then(() -> {
                pmeasure.count();
                latch.countDown();
            });
        }

        finished.await();
        Assert.assertTrue(successCount.get() == 10);

        callCount.set(0);
        try {
            Runtime.getRuntime().exec("firefox "+ new File("./src/main/test/kontraktor/remoting/httptest.html").getCanonicalPath() );
        } catch (IOException e) {
            e.printStackTrace();
        }
        Thread.sleep(10000);
        Assert.assertTrue(callCount.get() == 3);

        knode.getServer().stop();
        Thread.sleep(1000);
    }
}
