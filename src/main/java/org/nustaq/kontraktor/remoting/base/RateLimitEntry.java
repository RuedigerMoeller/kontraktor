package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.annotations.RateLimited;
import org.nustaq.kontraktor.util.Log;

import java.util.concurrent.TimeUnit;

public class RateLimitEntry {
    public static final long REJECT = -1l;

    private final RateLimited rateLimit;
    private final long interval;
    int count;
    long lastCheck;

    public RateLimitEntry(RateLimited rateLimited) {
        this.rateLimit = rateLimited;
        interval = rateLimited.callsPerMinute() != 0 ? TimeUnit.MINUTES.toMillis(1) : 1000;
    }

    public long registerCall(long now, String methodName) {
        count++;
        if ( lastCheck == 0 )
            lastCheck = now;
        else if ( now-lastCheck > interval ) {
            count = 0;
            lastCheck = 0;
        } else { // inside interval
            int maxCount = rateLimit.callsPerMinute() > 0 ? rateLimit.callsPerMinute() : rateLimit.callsPerSecond();
            if ( count >= maxCount ) {
                Log.Warn(this, "Ratelimit hit on "+methodName);
                return REJECT;
            }
        }
        return 0l;
    }
}
