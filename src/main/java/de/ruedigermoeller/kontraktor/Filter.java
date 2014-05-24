package de.ruedigermoeller.kontraktor;

/**
 * Created by ruedi on 23.05.14.
 */
public interface Filter<IN,OUT> {

    public Future<OUT> filter( IN result, Object error );

}
