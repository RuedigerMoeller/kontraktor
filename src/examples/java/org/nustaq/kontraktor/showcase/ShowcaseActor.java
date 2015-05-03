package org.nustaq.kontraktor.showcase;

import org.nustaq.kontraktor.*;
import org.nustaq.kontraktor.annotations.CallerSideMethod;
import org.nustaq.kontraktor.annotations.Local;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Created by ruedi on 01.05.2015.
 */
public class ShowcaseActor extends Actor<ShowcaseActor> {

    volatile int syncState = 99;

    ArrayList<String> stuff = new ArrayList<>();

    public void $simpleAsyncCall(String argument) {
        stuff.add(argument);
    }

    ///////////////////////////////////////////////////////////////////////////////////////////////
    //
    // Promises, promises, await, race, all
    //

    public IPromise<Integer> $indexOfPromise(String what) {
        return resolve(stuff.indexOf(what));
    }

    public IPromise<Integer> $combinePromise0( IPromise<Integer> a, IPromise<Integer> b) {
        Promise<Integer> result = new Promise<>(); // unresolved

        a.then( aresult -> b.then( bresult -> {
            result.resolve( Math.max( aresult, bresult ) );
        }));

        return result;
    }

    public IPromise<Integer> $combinePromise1( IPromise<Integer> a, IPromise<Integer> b) {
        Promise<Integer> result = new Promise<>(); // unresolved

        all(a,b).then(resArr -> result.resolve(Math.max(resArr[0].get(), resArr[1].get())));

        return result;
    }

    public IPromise<Integer> $combinePromise11( IPromise<Integer> a, IPromise<Integer> b) {
        return resolve(awaitAll(a, b).max((va, vb) -> va - vb).get());
    }

    public IPromise<Integer> $combinePromise2( IPromise<Integer> a, IPromise<Integer> b) {
        return resolve( Math.max( a.await(), b.await() ) );
    }

    public IPromise<Integer> $racePromise0( IPromise<Integer> a, IPromise<Integer> b) {
        return resolve( race(a, b).await() );
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    //
    // chaining, error handling, isolate blocking operations, timed scheduling
    //

    public IPromise<String> $getUrl(URL url) {
        return exec( () -> new Scanner(url.openStream(), "UTF-8").useDelimiter("\\A").next() );
    }

    public void $thenVariations0( URL urlA, URL urlB ) {
        $getUrl(urlA).then(cont -> {
            System.out.println("A receiced");
            self().$getUrl(urlB).then(contentB -> System.out.println("Got B result"));
        })
        .then(() -> System.out.println("hi there"));
    }

    public void $thenVariations1( URL urlA, URL urlB, URL urlC ) {
        $getUrl(urlA).thenAnd( contA -> {
            System.out.println("A received");
            return new Promise(self().$getUrl(urlB));
        })
        .thenAnd(contentB -> {
            System.out.println("B received");
            return self().$getUrl(urlC);
        })
        .then(contentC -> {
            System.out.println("C received");
        })
        .catchError(err -> {
            System.out.println("error:" + err);
        });
    }

    public void $thenVariations1UsingAwait( URL urlA, URL urlB, URL urlC ) {
        try {
            self().$submit(() -> System.out.println("I am running"));
            String a = $getUrl(urlA).await(5000);
            String b = $getUrl(urlB).await(5000);
            String c = $getUrl(urlC).await(5000);
        } catch (KTimeoutException timeout) {
            System.out.println("timeout");
        } catch ( AwaitException ex ) {
            System.out.println(ex.getError());
        }
    }

    public void $thenVariations1StartAllConcurrentAndWaitForAll( URL urlA, URL urlB, URL urlC ) {
        try {
            awaitAll( $getUrl(urlA), $getUrl(urlB), $getUrl(urlC) )
                .forEach( System.out::println );
        } catch (KTimeoutException timeout) {
            System.out.println("timeout");
        } catch ( AwaitException ex ) {
            System.out.println(ex.getError());
        }
    }

    public void $timedExecutionLoop(String output) {
        delayed( 1000, () -> $timedExecutionLoop(output) );
        System.out.println(output);
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // callbacks / streaming
    //

    public void $match(String toMatch, Callback<String> cb) {
        stuff.forEach( e -> {
            if (e.indexOf(toMatch) >= 0) {
                cb.stream(e);
            }
        });
        cb.finish();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // spores
    //

    public void $sporeDemoFullList( Spore<List<String>,Object> spore ) {
        spore.remote(stuff);
        spore.finish();
    }

    public void $sporeDemoIterating( Spore<String,String> spore ) {
        for (int i = 0; i < stuff.size() && ! spore.isFinished(); i++) {
            String s = stuff.get(i);
            spore.remote(s);
        }
        spore.finish();
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // transactions/ordered processing
    //

    ////////////////////////////////////////////////////////////////////////////////////////////////////
    //
    // synchronous access, threading trickery
    //

    @CallerSideMethod @Local
    public int getSyncState() {
        return getActor().syncState;
    }


}
