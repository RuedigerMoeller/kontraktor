package org.nustaq.kontraktor.apputil;

import org.nustaq.kontraktor.Actor;
import org.nustaq.kontraktor.annotations.CallerSideMethod;

import java.util.concurrent.atomic.AtomicInteger;

public interface UniqueSessionIntIdMixin<SELF extends Actor<SELF>> {

    AtomicInteger count = new AtomicInteger(1);
    int MAX_CONC_SESSIONS = 1_000_000;

    default void initUniqueSessionIntIdMixin() {
        if ( _getUnqiqueIntSessionId() == 0 ) {
            int id = count.incrementAndGet();
            _setUnqiqueInSessionId(id);
            if ( count.get() == MAX_CONC_SESSIONS ) {
                count.compareAndSet(MAX_CONC_SESSIONS,1);
            }
        }
    }

    SELF getActor();

    /**
     * call this for auto init
     * @return
     */
    @CallerSideMethod
    default int getIntSessionId() {
        initUniqueSessionIntIdMixin();
        return _getUnqiqueIntSessionId();
    }

    @CallerSideMethod
    int _getUnqiqueIntSessionId();
    @CallerSideMethod
    void _setUnqiqueInSessionId(int id);

}
