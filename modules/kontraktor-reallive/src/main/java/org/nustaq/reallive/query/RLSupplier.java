package org.nustaq.reallive.query;

import java.io.Serializable;
import java.util.function.Supplier;

/**
 * Created by ruedi on 29/08/15.
 */
public interface RLSupplier<T> extends Supplier<T>, Serializable {
}
