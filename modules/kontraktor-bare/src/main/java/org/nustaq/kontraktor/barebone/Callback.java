package org.nustaq.kontraktor.barebone;

import java.io.Serializable;

/**
 * Created by ruedi on 05/06/15.
 *
 * need to define as abstract class to be able to register a serializer ..
 *
 */
public abstract class Callback<T> implements Serializable {
    public abstract void receive(T result, Object error);
}
