package org.nustaq.kontraktor.wapi;

import org.nustaq.kontraktor.util.RateMeasure;

/**
 * Created by ruedi on 07.03.17.
 */
public interface ApiConstraints {

    /**
     * @param methodName
     * @return - maximum allowed calls per second of given method
     */
    double getMaxCallsPerSecond( String methodName );

    /**
     *
     * @param methodName
     * @return - return the length of the interval the limit is actually checked.
     *           Example: rate = 1 call per second, checked every 10 second means one can do 10 calls in one second and pause for 9.
     */
    long   getGraceInterval(String methodName);

    /**
     *
     * @return the maximum number of calls accepted until the GraceInterval is reached
     */
    int    getMaxOverload(String methodName);

}
