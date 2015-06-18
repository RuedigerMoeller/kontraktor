package service.server;

import org.nustaq.kontraktor.Actors;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;
import org.nustaq.kontraktor.remoting.encoding.SerializerType;
import org.nustaq.kontraktor.remoting.tcp.NIOServerConnector;
import org.nustaq.kontraktor.remoting.tcp.TCPNIOPublisher;
import service.common.MyServiceInterface;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import static service.common.MyProtocol.*;

/**
 * Created by ruedi on 19/05/15.
 */
public class MyServiceServerImpl extends MyServiceInterface<MyServiceServerImpl> {

    List<Person> persons = new ArrayList<>();

    @Override
    public void addPerson(Person p) {
        persons.add(p);
    }

    @Override
    public IPromise<Boolean> removePerson(Person p) {
        for (Iterator<Person> iterator = persons.iterator(); iterator.hasNext(); ) {
            Person next = iterator.next();
            if ( next.equals(p) ) {
                return resolve(true);
            }
        }
        return resolve(false);
    }

    @Override
    public IPromise<Boolean> existsPerson(Person p) {
        return resolve(persons.stream().filter(person -> person.equals(p)).findFirst().get() != null);
    }

    @Override
    public void listPersons(String preName, String name, int age, Callback<Person> result) {
        persons.forEach( person -> {
            if ( ( preName == null || preName.equals(person.getPreName()) ) &&
                 ( age <= 0 || age == person.getAge() ) &&
                 ( name == null || name.equals(person.getName() ) )
             ) {
                result.stream(person);
            }
        });
        result.finish();
    }

    public static void main( String a[] ) {
        MyServiceInterface myService = Actors.AsActor(MyServiceServerImpl.class, 128_000);// give large queue to service

        new TCPNIOPublisher()
            .facade(myService)
            .port(6789)
            .serType(SerializerType.FSTSer)
            .publish().await();

        System.out.println("server started on "+6789);
    }

}
