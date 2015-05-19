package service.common;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.Callback;
import org.nustaq.kontraktor.IPromise;

import static service.common.MyProtocol.*;

/**
 * Created by ruedi on 19/05/15.
 *
 * "Pseudo-Abstract" class defining the accessible (async) interface of the service/actor
 *
 * Currently for technical reasons this "Interface" class cannot be abstract nor can it be made an interface
 * (trouble with byte code generation). However that's just a minor inconvenience ..
 *
 */
public class MyServiceInterface<T extends MyServiceInterface> extends Actor<T> {

    public void $addPerson( Person p ) {}
    public IPromise<Boolean> $removePerson( Person p ) { return null; }
    public IPromise<Boolean> $existsPerson( Person p ) { return null; }

    /**
     * cannot use a spore here as client and server are not supposed to share a common clathpath
     * @param name
     * @param secondName
     * @param age
     * @param result
     */
    public void $listPersons( String name, String secondName, int age, Callback<Person> result ) {}

}
