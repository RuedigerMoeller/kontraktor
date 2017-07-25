package org.nustaq.reallive.impl.actors;

import java.io.Serializable;

/**
 * Created by moelrue on 06.08.2015.
 */
public interface ShardFunc extends Serializable {
    int apply(String key);
}
