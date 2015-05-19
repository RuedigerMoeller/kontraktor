package service.common;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

import java.util.Collection;
import java.util.List;
import static service.common.MyProtocol.*;

/**
 * Created by ruedi on 19/05/15.
 *
 * Abstract class defining the accessible (async) interface of the service/actor
 *
 */
public class MyService<T extends MyService> extends Actor<T> {

    public void $addPerson( Person p ) {}
    public IPromise<Boolean> $removePerson( Person p ) { return null; }
    public IPromise<Boolean> $existsPerson( Person p ) { return null; };

    /**
     * cannot use a spore here as client and server are not supposed to share a common clathpath
     * @param name
     * @param secondName
     * @param age
     * @param result
     */
    public void $listPersons( String name, String secondName, int age, Callback<Person> result ) {}

}
