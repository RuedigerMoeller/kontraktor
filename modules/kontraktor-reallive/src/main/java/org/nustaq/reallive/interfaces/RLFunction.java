package org.nustaq.reallive.interfaces;

import java.io.Serializable;
import java.util.function.Function;

/**
 * Created by ruedi on 14.07.16.
 */
public interface RLFunction<A,B> extends Function<A,B>, Serializable {
}
