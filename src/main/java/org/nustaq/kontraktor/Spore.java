package org.nustaq.kontraktor;

import org.nustaq.kontraktor.impl.CallbackWrapper;
import org.nustaq.serialization.annotations.AnonymousTransient;

import java.io.Serializable;

/**
 * Created by ruedi on 26.08.2014.
 * <pre>
 * int x = 100;
 * int y = 111;
 * String s = "pok";
 *
 * public Spore<Integer,String> getSpore(int z) {
 *     return new Spore<Integer,String>() {
 *         // declare
 *         int sx,sy,sz;
 *         HashMap map;
 *
 *         {
 *             // capture
 *             sx = x; sy = y; sz = z;
 *             map = new HashMap();
 *         }
 *
 *         // remotely executed
 *         public void body(Integer in, Callback<String> out) {
 *             System.out.println("executed later " + sx + " " + sy + " " + sz);
 *         }
 *     };
 * }
 * </pre>
 */
@AnonymousTransient
public abstract class Spore<I,O> implements Serializable {

    Callback cb;

    public Spore() {
        Callback mycb = new Callback() {
            @Override
            public void receive(Object result, Object error) {
                local( (O)result, error );
            }
        };
        this.cb = new CallbackWrapper<>(Actor.sender.get(),mycb);
    }

    public abstract void remote( I input );
    public abstract void local(O result, Object error);

    public void finished() {
        // signal finish of execution, so remoting can clean up callback id mappings
        // override if always single result or finish can be emitted by the remote method
        // note one can send FIN to avoid the final message to visible to receiver callback/spore
        cb.receive(null, Callback.FINSILENT);
    }

    protected void receive(O result, Object err) {
        cb.receive(result, err);
    }

    public Callback<O> getCb() {
        return cb;
    }
}
