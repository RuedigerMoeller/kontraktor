package wapi;

import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.http.HttpConnectable;

/**
 * Created by ruedi on 10.03.17.
 */
public class DummyClient {
    public static void main(String[] args) {
        HttpConnectable con = new HttpConnectable(DummyService.class,"http://localhost:7777/dummyservice").coding(new Coding(SerializerType.JsonNoRef));
        DummyService connect = (DummyService) con.connect((x, y) -> System.out.println("" + x + y)).await();
        connect.ask("service","hello").then( (x,y) -> System.out.println(x));
    }
}
