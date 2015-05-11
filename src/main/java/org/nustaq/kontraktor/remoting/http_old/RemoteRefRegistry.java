package org.nustaq.kontraktor.remoting.http_old;

import org.nustaq.kontraktor.remoting.base.RemoteRegistry;
import org.nustaq.kontraktor.remoting.base.ObjectSocket;
import org.nustaq.kontraktor.remoting.encoding.Coding;
import org.nustaq.kontraktor.util.Log;

import java.io.EOFException;
import java.net.SocketException;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Created by ruedi on 08.08.14.
 *
 * tracks remote references of a single point to point connection
 *
 * fixme: handle stop of published actor (best by talking back in case a message is received on a
 * stopped published actor).
 *
 * @Deprecated, phased out (redesign)
 */
public abstract class RemoteRefRegistry extends RemoteRegistry {

    public RemoteRefRegistry() {
		this(null);
	}

    public RemoteRefRegistry(Coding code) {
        super(code);
    }

    protected void receiveLoop(OldObjectSocket channel) {
        try {
            while (!isTerminated()) {
                if (singleReceive(channel)) continue;
            }
        } catch (SocketException soc) {
            Log.Lg.warn(this, "" + soc);
        } catch (EOFException eof) {
            Log.Lg.warn(this, "" + eof);
        } catch (Throwable e) {
            Log.Lg.error(this, e, "");
        } finally {
            cleanUp();
        }
    }

    /**
     *
     * @param channel
     * @return true if no message could be read (either failure or nonblocking channel)
     * @throws Exception
     */
    public boolean singleReceive(OldObjectSocket channel) throws Exception {
        // read object
        final Object response = channel.readObject();
        return receiveObject(channel, response);
    }

    public abstract AtomicReference<OldObjectSocket> getObjectSocket();

    @Override
    public AtomicReference<ObjectSocket> getWriteObjectSocket() {
        return ((AtomicReference) getObjectSocket());
    }

}


