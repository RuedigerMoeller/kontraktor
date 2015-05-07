package org.nustaq.kontraktor.remoting;

import org.nustaq.kontraktor.util.Log;
import org.nustaq.serialization.FSTConfiguration;

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

    public static final Object OUT_OF_ORDER_SEQ = "OOOS";

    public RemoteRefRegistry() {
		this(null);
	}

    public RemoteRefRegistry(Coding code) {
        super(code);
    }

    protected void receiveLoop(ObjectSocket channel) {
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
    public boolean singleReceive(ObjectSocket channel) throws Exception {
        // read object
        final Object response = channel.readObject();
        return receiveObject(channel, response);
    }

    public abstract AtomicReference<ObjectSocket> getObjectSocket();

    @Override
    public AtomicReference<WriteObjectSocket> getWriteObjectSocket() {
        return ((AtomicReference) getObjectSocket());
    }

}


