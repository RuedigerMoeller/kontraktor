package remoting;

import kontraktor.RemotingTest;
import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.base.JsonMapable;
import org.nustaq.kontraktor.remoting.http.undertow.Http4K;

public class RESTTest {

    public static class TestPojo implements JsonMapable {
        String name;
        int x;
        double y;

        @Override
        public String toString() {
            return "TestPojo{" +
                "name='" + name + '\'' +
                ", x=" + x +
                ", y=" + y +
                '}';
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }

        public double getY() {
            return y;
        }

        public void setY(double y) {
            this.y = y;
        }
    }

    public static class RESTActor extends Actor<RESTActor> {

        public IPromise getHello(String path ) {
            return resolve("Hello "+path);
        }

        public IPromise postPost( TestPojo pojo ) {
            return resolve("Hello "+pojo);
        }
    }

    public static void main(String[] args) {
        System.out.println(JsonMapable.class.isAssignableFrom(TestPojo.class));

        RESTActor act = Actors.AsActor(RESTActor.class);
        Http4K.Build("localhost",9999)
            .restAPI("/", act )
            .build();
    }

}
