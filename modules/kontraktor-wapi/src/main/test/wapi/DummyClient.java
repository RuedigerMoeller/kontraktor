package wapi;

import org.nustaq.kontraktor.barebone.Callback;
import org.nustaq.kontraktor.barebone.RemoteActor;
import org.nustaq.kontraktor.barebone.RemoteActorConnection;

/**
 * Created by ruedi on 10.03.17.
 */
public class DummyClient {
    RemoteActorConnection actorConnection;
    RemoteActor facade;

    public boolean isConnected() {
        return actorConnection != null && facade != null;
    }

    public void connect() {
        System.out.println("try connecting ..");
        actorConnection = new RemoteActorConnection( s -> {
            disconnect();
        });
        try {
            facade = actorConnection.connect("http://localhost:7777/dummyservice", true).await();
            run();
        } catch (Throwable e) {
            e.printStackTrace();
            disconnect();
        }
    }

    private void disconnect() {
        System.out.println("connection closed");
        actorConnection = null;
        facade = null;
        new Thread(()->{
            try {
                Thread.sleep(5000);
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            connect();
        }).start();
    }

    public void run() {
        facade.ask("service", "hello" ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.ask("foreign", new ForeignClass(1,2,3) ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.ask("service", "holla" ).then(
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
        facade.tell("subscribe", new ForeignClass(1,2,3),
            new Callback() {
                @Override
                public void receive(Object result, Object error) {
                    System.out.println(result+" "+error);
                }
            }
        );
    }

    public static void main(String[] args) throws InterruptedException {
        DummyClient dummyClient = new DummyClient();
        dummyClient.connect();
    }
}
