package wapi;

import org.nustaq.kontraktor.barebone.*;

/**
 * Created by ruedi on 03.03.17.
 */
public class TestClient {

    public static void main(String[] args) {
        RemoteActorConnection act = new RemoteActorConnection(
           new ConnectionListener() {
               @Override
               public void connectionClosed(String s) {
                   System.out.println("connection closed");
               }
           },
           false
        );

        act.connect("http://localhost:7777/api", true).then(new Callback<RemoteActor>() {
            @Override
            public void receive(RemoteActor facade, Object error) {
              facade.ask("hello", "Rüdi").then(new Callback() {
                  @Override
                  public void receive(Object result, Object error) {
                    System.out.println("hello "+result);
                  }
              });
              facade.ask("verify", "MA6cia2FyhHKy4pD8hY+8Q==").then(new Callback() {
                  @Override
                  public void receive(Object result, Object error) {
                    System.out.println("verify "+result);
                  }
              });
            }
        });
    }
}
