package org.nustaq.kontraktor.remoting.base;

public class RateLimitException extends RuntimeException {
    public  RateLimitException() {
        super("Ratelimit hit");
    }

}
