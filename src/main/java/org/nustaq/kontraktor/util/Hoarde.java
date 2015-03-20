package org.nustaq.kontraktor.util;

import org.nustaq.kontraktor.*;
import org.nustaq.serialization.FSTConfiguration;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Created by ruedi on 07.09.14.
 *
 * a utility class allowing to address/manage multiple actors of same type
 *
 */
public class Hoarde<T extends Actor> {

    Actor actors[];
    int index = 0;
    Promise prev;

    /**
     * create a hoarde with each actor having a dedicated thread
     * @param numActors
     * @param actor
     */
    public Hoarde(int numActors, Class<T> actor) {
        actors = new Actor[numActors];
        for (int i = 0; i < actors.length; i++) {
            actors[i] = Actors.AsActor(actor);
        }
    }

    /**
     * create a hoarde scheduled on given scheduler
     * @param numActors
     * @param actor
     * @param sched
     */
    public Hoarde(int numActors, Class<T> actor, Scheduler sched) {
        actors = new Actor[numActors];
        for (int i = 0; i < actors.length; i++) {
            actors[i] = Actors.AsActor(actor, sched);
        }
    }

    public Future[] map(BiFunction<T, Integer, Future> init) {
        Future res[] = new Future[actors.length];
        for (int i = 0; i < actors.length; i++) {
            T actor = (T) actors[i];
            res[i] = init.apply(actor,i);
        }
        return res;
    }

    /**
     * iterate over each actor and execute tocall. E.g. hoarde.each( actor -> actor.$init() )
     * @param tocall
     * @return
     */
    public Hoarde<T> each(Consumer<T> tocall) {
        for (int i = 0; i < actors.length; i++) {
            tocall.accept( (T) actors[i] );
        }
        return this;
    }

    /**
     * same as other each but with index
     *
     * @param init
     * @return
     */
    public Hoarde<T> each(BiConsumer<T, Integer> init) {
        for (int i = 0; i < actors.length; i++) {
            init.accept( (T) actors[i], i );
        }
        return this;
    }

    /**
     * calls given function round robin. typical use:
     *
     * hoarde.ordered( actor -> actor.decode(byte[]) ).onResult( decodedObj -> businesslogic(decodedObj) );
     *
     * after
     * @param toCall
     * @return
     */
    public Future ordered(Function<T, Future> toCall) {
        final Future result = toCall.apply((T) actors[index]);
        index++;
        if (index==actors.length)
            index = 0;
        if ( prev == null ) {
            prev = new Promise();
            result.then(prev);
            return prev;
        } else {
            Promise p = new Promise();
            prev.getNext().finishWith( (res, err) -> result.then( (res1,err1) -> p.receive(res1, err1) ) );
            prev = p;
            return p;
        }
    }

    public int getSize() {
        return actors.length;
    }

    public T getActor(int i) {
        return (T) actors[i];
    }

}
