package websockets;

import encoding.TestEncoding;
import org.nustaq.kontraktor.annotations.GenRemote;
import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.RemoteActorInterface;
import org.nustaq.kontraktor.remoting.Coding;
import org.nustaq.kontraktor.remoting.SerializerType;
import org.nustaq.kontraktor.remoting.http.netty.wsocket.ActorWSServer;
import org.nustaq.kontraktor.remoting.http.netty.wsocket.WSocketActorClient;
import org.nustaq.netty2go.NettyWSHttpServer;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by ruedi on 31.08.14.
 */
@GenRemote
public class WSTestActor extends Actor<WSTestActor> {

	public static class SampleUser { // test using javaclass from js
		String name;
		int age;
		int credits;
		ArrayList<String> roles = new ArrayList<>(0);

		public SampleUser(String name, int age, int credits) {
			this.name = name;
			this.age = age;
			this.credits = credits;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public int getAge() {
			return age;
		}

		public void setAge(int age) {
			this.age = age;
		}

		public int getCredits() {
			return credits;
		}

		public void setCredits(int credits) {
			this.credits = credits;
		}

		public ArrayList<String> getRoles() {
			return roles;
		}

		public void setRoles(ArrayList<String> roles) {
			this.roles = roles;
		}
	}

	public static class ClientSession extends Actor<ClientSession> {

		WSTestActor parent;
		SampleUser user;
		JSActorInterface remoteClientActor;

		public void $init(WSTestActor parent) {
			this.parent = parent;
		}

		public Future<String> $clientSpecific(String clientId, String whatNot ) {
			return new Promise<>(clientId+" and "+whatNot);
		}

		public void $setUser( SampleUser user ) {
			this.user = user;
		}

		public Future<SampleUser> $getUser() {
			return new Promise<>(user);
		}

		public Future $print(String s) {
			System.out.println("Hello--------------------------------------------------------");
			return new Promise("yes");
		}

		public void $pingRound(ClientSession sess) {
			sess.$print("TestToken").then((r, e) -> {
				System.out.println("printres " + r);
				remoteClientActor.$testFinished();
			});
		}

		public void $registerClientActor(JSActorInterface actor) {
			remoteClientActor = actor;
			$sendLoop(0);
		}

		public void $sendLoop(int count) {
			remoteClientActor.$voidCallClient("Hello Client "+count);
			long nanos = System.currentTimeMillis();
			remoteClientActor.$futureCall("Hello Client "+count)
			   .then( (r,e) -> System.out.println("round trip "+(System.currentTimeMillis()-nanos)) );
//			delayed(2000, () -> $sendLoop(count+1) );
		}
	}

	@RemoteActorInterface
	public static class JSActorInterface extends Actor<JSActorInterface> {

		public void $voidCallClient(String s) {}
		public Future $futureCall(String s) { return null; }
		public void $testFinished() {}

	}

    public void $voidCall(String message) {
        System.out.println("receive msg:"+message);
    }

    public Future $futureCall(String result) {
        System.out.println("futcall "+result );
        return new Promise<>(result);
    }

	public Future<ClientSession> $createSession() {
		return new Promise<>(Actors.AsActor(ClientSession.class,getScheduler()));
	}

	public Future<TestEncoding> $createTestEncoding() {
		return new Promise<>(Actors.AsActor(TestEncoding.class,getScheduler()));
	}

    public void $callbackTest(String msg, Callback cb) {
        System.out.println("cb call "+msg);
        cb.receive(msg,null);
    }


    public static void main(String arg[]) throws Exception {
        if ( arg.length == 0 ) {
            startServer();
        } else {
            startClient();
        }
    }

    private static void startClient() throws Exception {
        WSocketActorClient cl = new WSocketActorClient(WSTestActor.class,"ws://localhost:8887/websocket", new Coding(SerializerType.MinBin));
        cl.connect();
        WSTestActor facadeProxy = (WSTestActor) cl.getFacadeProxy();

        int count =0;
        while( count++ < 5 ) {
            facadeProxy.$voidCall("Hello void");
            Thread.sleep(1000);
            facadeProxy.$futureCall("hello future").then((r, e) -> System.out.println("future call worked result:" + r));
            Thread.sleep(1000);
            facadeProxy.$callbackTest("hello cb", (r,e) -> System.out.println("cb result "+r) );
            Thread.sleep(1000);
        }

        facadeProxy.$close();
    }

    private static void startServer() throws Exception {
        Actor actor = Actors.AsActor(WSTestActor.class);
        ActorWSServer server = new ActorWSServer( actor, new File("./"), new Coding(SerializerType.MinBin) );
	    server.setFileMapper( (f) -> {
		    if ( f != null && f.getName() != null ) {
			    if ( f.getName().equals("minbin.js") ) {
				    File file = new File("C:\\work\\GitHub\\fast-serialization\\src\\main\\javascript\\minbin.js");
				    if ( ! file.exists() ) {
	                    return new File("/home/ruedi/IdeaProjects/fast-serialization/src/main/javascript/minbin.js");
				    }
				    return file;
			    }
		    }
		    return f;
	    });
        new NettyWSHttpServer(8887, server).run();
    }
}
