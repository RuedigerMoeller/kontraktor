package de.ruedigermoeller.kontraktor;

/**
 * Created by ruedi on 23.05.14.
 * 
 * works like 'then' but passes the future returned to the next filter() or then() call in a future chain.
 */
public interface Filter<IN,OUT> {

    public Future<OUT> filter( IN result, Object error );

}
