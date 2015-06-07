package org.nustaq.reallive;

import java.util.function.Predicate;

/**
 * Created by ruedi on 20.07.14.
 */
public interface Subscription<T extends Record> {

    public String getTableKey();

    public Predicate<T> getFilter();

}
