package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.serialization.annotations.AnonymousTransient;

import java.io.Serializable;

/**
 * Created by ruedi on 26.08.2014.
 *
 * A Spore is sent to a foreign actor executes on its data and sends results back to caller
 */
@AnonymousTransient
public abstract class Spore<I,O> implements Serializable {

    Callback cb;
    transient protected boolean finished;
    transient Callback<O> localCallback;

    public Spore() {
        Callback mycb = new Callback() {
            @Override
            public void receive(Object result, Object error) {
                local( (O)result, error );
            }
        };
        this.cb = new CallbackWrapper<>(Actor.sender.get(),mycb);
    }

    /**
     * implements code to be executed at receiver side
     * @param input
     */
    public abstract void remote( I input );

    /**
     * is called on sender side once results stream in from receiver side remote() method.
     * Preferably do not override this but register a callback on sender side using 'then()'
     * @param result
     * @param error
     */
    public void local(O result, Object error) {
        if ( localCallback != null ) {
            localCallback.receive(result,error);
        } else {
            System.err.println("override local() method or set callback using then() prior sending");
        }
    }

    /**
     * local. Register at sending side and will recieve data streamed back from remote.
     * Aalternatively one overriding local(..)
     * @param cb
     * @return
     */
    public Spore<I,O> then( Callback<O> cb ) {
        localCallback = cb;
        return this;
    }

    public void finished() {
        // signal finish of execution, so remoting can clean up callback id mappings
        // override if always single result or finish can be emitted by the remote method
        // note one can send FIN to avoid the final message to visible to receiver callback/spore
        cb.receive(null, Callback.FINSILENT);
        finished = true;
    }

    protected void receive(O result, Object err) {
        cb.receive(result, err);
    }

    /**
     * to be read at remote side in order to decide wether to stop e.g. iteration.
     * @return
     */
    public boolean isFinished() {
        return finished;
    }
}
