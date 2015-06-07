package org.nustaq.reallive.impl;

import org.nustaq.offheap.bytez.Bytez;
import org.nustaq.reallive.impl.storage.BinaryStorage;

/**
 * Created by ruedi on 21.06.14.
 */
public interface IdGenerator<K> {
    K nextid();

    /**
     * return length used.
     * @return
     */
    int setState( BinaryStorage storage );
}
