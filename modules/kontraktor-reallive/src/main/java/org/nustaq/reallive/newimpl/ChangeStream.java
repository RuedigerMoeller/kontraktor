package org.nustaq.reallive.newimpl;

import java.util.function.*;

/**
 * Created by moelrue on 03.08.2015.
 */
public interface ChangeStream {

    public void subscribe( Predicate<Record> filter, ChangeReceiver rec );

}
