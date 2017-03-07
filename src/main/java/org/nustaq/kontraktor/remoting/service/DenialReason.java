package org.nustaq.kontraktor.remoting.service;

/**
 * Created by ruedi on 06.03.17.
 */
public enum DenialReason {
    RATE_LIMIT_EXCEEDED,
    ILLEGAL_METHOD_OR_PARAMETER_TYPES,
    INVALID_TOKEN,
    INVALID_USER,
}
