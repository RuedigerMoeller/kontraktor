package org.nustaq.reallive.old;

import org.nustaq.reallive.old.storage.BinaryStorage;

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
