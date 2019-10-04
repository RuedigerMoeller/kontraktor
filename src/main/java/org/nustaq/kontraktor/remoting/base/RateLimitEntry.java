package org.nustaq.kontraktor.remoting.base;

import org.nustaq.kontraktor.annotations.RateLimited;

public class RateLimitEntry {
    private final RateLimited rateLimit;
    int count;
    long lastCheck;

    public RateLimitEntry(RateLimited rateLimited) {
        this.rateLimit = rateLimited;
    }
}
