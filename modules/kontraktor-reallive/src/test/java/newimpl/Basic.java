package newimpl;

import org.junit.Test;
import org.nustaq.reallive.newimpl.ChangeRequestBuilder;
import org.nustaq.reallive.newimpl.ChangeStreamImpl;
import org.nustaq.reallive.newimpl.HeapStore;

/**
 * Created by ruedi on 04.08.2015.
 */
public class Basic {

    @Test
    public void test() {
        ChangeStreamImpl stream = new ChangeStreamImpl(new HeapStore<>());
        stream.listener( chaneg -> {
            System.out.println(chaneg);
        });
        ChangeRequestBuilder cb = ChangeRequestBuilder.get();
        stream.receive(cb.add("one",
            "name", "emil",
            "age", 9
        ));
        stream.receive(cb.add("two",
            "name", "felix",
            "age", 17
        ));
        stream.receive( cb.update("one", "age", 10) );
        stream.receive( cb.remove("one"));
        stream.getStore().forEach( rec -> {
            System.out.println(rec);
        });
    }
}
