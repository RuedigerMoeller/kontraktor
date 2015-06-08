package org.nustaq.reallive;

import org.nustaq.reallive.storage.BinaryStorage;

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
