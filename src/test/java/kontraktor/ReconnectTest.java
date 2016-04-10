package kontraktor;

import org.junit.Ignore;
import org.junit.Test;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.remoting.tcp.TCPClientConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPConnectable;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import org.nustaq.kontraktor.util.Log;

/**
 * Created by ruedi on 4/10/2016.
 */
public class ReconnectTest {

    public static class ServiceA extends Actor<ServiceA> {
        public IPromise promise(int val) {
            Log.Info(this,"pinged "+val);
            return resolve(val);
        }
        @Override
        protected void __stopImpl() {
            // do not stop impl if remoteproxy, just buffer ..
        }
    }

    public static class ClientA extends Actor<ClientA> {

        ServiceA service;
        int count;

        public void init(int port) {
            service = (ServiceA) new TCPConnectable(ServiceA.class, "localhost", port)
                .connect((acc, err) -> System.out.println("disconnect " + acc + " " + err), actor -> System.out.println("discon 1 " + actor))
                .await();
            Log.Info(this,"connected");
            pingLoop();
        }

        public void pingLoop() {
            service.promise(count++);
            delayed(1000,()->pingLoop());
        }

    }

    @Test @Ignore
    public void test() throws InterruptedException {
        ServiceA serviceA = Actors.AsActor( ServiceA.class );
        new TCPNIOPublisher(serviceA,5678).publish(actor -> System.out.println("server side discon "+actor) );
        ClientA cl = Actors.AsActor(ClientA.class);
        cl.init(5678);
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test @Ignore
    public void runClient() throws InterruptedException {
        ClientA cl = Actors.AsActor(ClientA.class);
        cl.init(5678);
        Thread.sleep(Integer.MAX_VALUE);
    }

    @Test @Ignore
    public void runService() throws InterruptedException {
        ServiceA serviceA = Actors.AsActor( ServiceA.class );
        new TCPNIOPublisher(serviceA,5678).publish(actor -> System.out.println("server side discon "+actor) );
        Thread.sleep(Integer.MAX_VALUE);
    }
}
