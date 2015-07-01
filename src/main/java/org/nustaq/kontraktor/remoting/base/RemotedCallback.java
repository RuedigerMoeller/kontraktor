package org.nustaq.kontraktor.remoting.base;

/**
 * Created by ruedi on 01/07/15.
 *
 * Tagging interface for callbacks forwarding to a remote location. A remoted callback is
 * deserialized as CallbackWrapper with the "realCallback" field containing an
 * instance of RemotedCallback
 *
 */
public interface RemotedCallback {

    boolean isTerminated();

}
